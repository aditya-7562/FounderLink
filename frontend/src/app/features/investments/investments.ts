import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { InvestmentService } from '../../core/services/investment.service';
import { StartupService } from '../../core/services/startup.service';
import { UserService } from '../../core/services/user.service';
import { InvestmentResponse, InvestmentStatus, PaginatedData, StartupResponse } from '../../models';
import { PaginationControlsComponent } from '../../shared/components/pagination-controls/pagination-controls';

@Component({
  selector: 'app-investments',
  imports: [CommonModule, FormsModule, PaginationControlsComponent],
  templateUrl: './investments.html',
  styleUrl: './investments.css'
})
export class InvestmentsComponent implements OnInit {
  investments       = signal<InvestmentResponse[]>([]);
  myStartups        = signal<StartupResponse[]>([]);
  startupPage       = signal<PaginatedData<StartupResponse>>({
    content: [],
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
    last: true
  });
  investmentPage    = signal<PaginatedData<InvestmentResponse>>({
    content: [],
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
    last: true
  });
  selectedStartupId = signal<number | null>(null);
  loading           = signal(true);
  updating          = signal<number | null>(null);
  errorMsg          = signal('');
  successMsg        = signal('');
  filterStatus      = '';
  userNames         = signal<Map<number, string>>(new Map());

  constructor(
    public authService: AuthService,
    private investmentService: InvestmentService,
    private startupService: StartupService,
    private userService: UserService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadMyStartups();

    this.userService.getAllUsers({ page: 0, size: 50, sort: 'id,asc' }).subscribe({
      next: env => {
        this.userNames.update(map => {
          const newMap = new Map(map);
          env.data?.content.forEach(u => {
            if (!newMap.has(u.userId)) {
              newMap.set(u.userId, u.name || u.email);
            }
          });
          return newMap;
        });
      }
    });
  }

  loadMyStartups(page = 0): void {
    this.startupService.getMyStartups({ page, size: 10, sort: 'createdAt,desc' }).subscribe({
      next: env => {
        const startupPage = env.data ?? this.startupPage();
        this.startupPage.set(startupPage);
        this.myStartups.set(startupPage.content);
        if (startupPage.content.length > 0) {
          const current = this.selectedStartupId();
          const selected = startupPage.content.find(s => s.id === current) ?? startupPage.content[0];
          this.selectedStartupId.set(selected.id);
          this.loadInvestments(selected.id, 0);
        } else {
          this.loading.set(false);
          this.investments.set([]);
          this.investmentPage.set({ ...this.investmentPage(), content: [], totalElements: 0, totalPages: 0, last: true, page: 0 });
        }
      },
      error: () => this.loading.set(false)
    });
  }

  onStartupChange(startupId: number): void {
    this.selectedStartupId.set(startupId);
    this.loadInvestments(startupId);
  }

  loadInvestments(startupId: number, page = 0): void {
    this.loading.set(true);
    this.investmentService.getStartupInvestments(startupId, { page, size: 10, sort: 'createdAt,desc' }).subscribe({
      next: env => {
        const investmentPage = env.data ?? this.investmentPage();
        this.investmentPage.set(investmentPage);
        this.investments.set(investmentPage.content);
        this.resolveNames(investmentPage.content.map(i => i.investorId));
        this.loading.set(false);
      },
      error: env => { this.errorMsg.set(env.error ?? 'Failed to load investments.'); this.loading.set(false); }
    });
  }

  private resolveNames(userIds: number[]): void {
    const current = this.userNames();
    const missing = Array.from(new Set(userIds)).filter(id => !current.has(id));

    missing.forEach(id => {
      this.userService.getUser(id).subscribe({
        next: env => {
          if (env.data) {
            this.userNames.update(map => {
              const newMap = new Map(map);
              newMap.set(id, env.data!.name || env.data!.email);
              return newMap;
            });
          }
        }
      });
    });
  }

  updateStatus(investmentId: number, status: 'APPROVED' | 'REJECTED'): void {
    this.updating.set(investmentId);
    this.errorMsg.set('');
    this.investmentService.updateStatus(investmentId, { status }).subscribe({
      next: env => {
        this.updating.set(null);
        const updated = env.data;
        if (updated) {
          this.investments.update(list => list.map(i => i.id === investmentId ? updated : i));
        }
        this.successMsg.set(`Investment ${status.toLowerCase()} successfully.`);
        setTimeout(() => this.successMsg.set(''), 3000);
      },
      error: env => {
        this.updating.set(null);
        this.errorMsg.set(env.error ?? 'Failed to update status.');
      }
    });
  }

  get filteredInvestments(): InvestmentResponse[] {
    return this.investments().filter(i => !this.filterStatus || i.status === this.filterStatus);
  }

  get totalAmount(): number  { return this.investments().reduce((s, i) => s + i.amount, 0); }
  get pendingCount(): number  { return this.investments().filter(i => i.status === 'PENDING').length; }
  get approvedCount(): number { return this.investments().filter(i => i.status === 'APPROVED').length; }
  get completedCount(): number { return this.investments().filter(i => i.status === 'COMPLETED').length; }

  statusClass(status: string): string {
    return status === 'APPROVED'        ? 'badge-success'
         : status === 'PENDING'         ? 'badge-warning'
         : status === 'COMPLETED'       ? 'badge-info'
         : status === 'STARTUP_CLOSED'  ? 'badge-gray'
         : 'badge-danger';
  }

  statusLabel(status: InvestmentStatus): string {
    const labels: Record<InvestmentStatus, string> = {
      PENDING: 'Pending Approval', APPROVED: 'Approved', REJECTED: 'Rejected',
      COMPLETED: 'Completed', PAYMENT_FAILED: 'Payment Failed', STARTUP_CLOSED: 'Startup Closed'
    };
    return labels[status] ?? status;
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(amount);
  }

  messageInvestor(investorId: number): void {
    this.router.navigate(['/dashboard/messages'], { queryParams: { user: investorId } });
  }

  nextStartupPage(): void {
    this.loadMyStartups(this.startupPage().page + 1);
  }

  previousStartupPage(): void {
    this.loadMyStartups(Math.max(this.startupPage().page - 1, 0));
  }

  nextInvestmentPage(): void {
    const startupId = this.selectedStartupId();
    if (startupId == null) return;
    this.loadInvestments(startupId, this.investmentPage().page + 1);
  }

  previousInvestmentPage(): void {
    const startupId = this.selectedStartupId();
    if (startupId == null) return;
    this.loadInvestments(startupId, Math.max(this.investmentPage().page - 1, 0));
  }
}
