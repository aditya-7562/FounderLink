import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { InvestmentService } from '../../../core/services/investment.service';
import { StartupService } from '../../../core/services/startup.service';
import { InvestmentResponse, InvestmentStatus, PaginatedData } from '../../../models';
import { PaginationControlsComponent } from '../../../shared/components/pagination-controls/pagination-controls';

@Component({
  selector: 'app-portfolio',
  imports: [CommonModule, FormsModule, RouterLink, PaginationControlsComponent],
  templateUrl: './portfolio.html',
  styleUrl: './portfolio.css'
})
export class PortfolioComponent implements OnInit {
  investments = signal<InvestmentResponse[]>([]);
  portfolioPage = signal<PaginatedData<InvestmentResponse>>({
    content: [],
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
    last: true
  });
  loading     = signal(true);
  errorMsg    = signal('');
  filterStatus = '';
  startupNames = signal<Map<number, string>>(new Map());
  founderIds   = signal<Map<number, number>>(new Map());

  constructor(
    public authService: AuthService,
    private investmentService: InvestmentService,
    private startupService: StartupService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadPortfolio();

    this.startupService.getAll({ page: 0, size: 50, sort: 'createdAt,desc' }).subscribe({
      next: env => {
        const nameMap = new Map<number, string>();
        const idMap   = new Map<number, number>();
        env.data?.content.forEach(s => {
          nameMap.set(s.id, s.name);
          idMap.set(s.id, s.founderId);
        });
        this.startupNames.set(nameMap);
        this.founderIds.set(idMap);
      }
    });
  }

  loadPortfolio(page = 0): void {
    this.investmentService.getMyPortfolio({ page, size: 10, sort: 'createdAt,desc' }).subscribe({
      next: env => {
        const portfolioPage = env.data ?? this.portfolioPage();
        this.portfolioPage.set(portfolioPage);
        this.investments.set(portfolioPage.content);
        this.loading.set(false);
      },
      error: env => { this.errorMsg.set(env.error ?? 'Failed to load portfolio.'); this.loading.set(false); }
    });
  }

  messageFounder(startupId: number): void {
    const founderId = this.founderIds().get(startupId);
    if (founderId) {
      this.router.navigate(['/dashboard/messages'], { queryParams: { user: founderId } });
    }
  }

  get filtered(): InvestmentResponse[] {
    return this.investments().filter(i => !this.filterStatus || i.status === this.filterStatus);
  }

  get totalInvested(): number   { return this.investments().reduce((s, i) => s + i.amount, 0); }
  get completedAmount(): number { return this.investments().filter(i => i.status === 'COMPLETED').reduce((s, i) => s + i.amount, 0); }
  get pendingAmount(): number   { return this.investments().filter(i => i.status === 'PENDING').reduce((s, i) => s + i.amount, 0); }
  get approvedAmount(): number  { return this.investments().filter(i => i.status === 'APPROVED').reduce((s, i) => s + i.amount, 0); }

  statusLabel(status: InvestmentStatus): string {
    const labels: Record<InvestmentStatus, string> = {
      PENDING: 'Pending Review', APPROVED: 'Approved', REJECTED: 'Rejected',
      COMPLETED: 'Completed', PAYMENT_FAILED: 'Payment Failed', STARTUP_CLOSED: 'Startup Closed'
    };
    return labels[status] ?? status;
  }

  statusClass(status: string): string {
    return status === 'APPROVED'       ? 'badge-success'
         : status === 'PENDING'        ? 'badge-warning'
         : status === 'COMPLETED'      ? 'badge-info'
         : status === 'STARTUP_CLOSED' ? 'badge-gray'
         : 'badge-danger';
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(amount);
  }

  nextPage(): void {
    this.loadPortfolio(this.portfolioPage().page + 1);
  }

  previousPage(): void {
    this.loadPortfolio(Math.max(this.portfolioPage().page - 1, 0));
  }
}
