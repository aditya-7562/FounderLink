import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { StartupService } from '../../../core/services/startup.service';
import { InvestmentService } from '../../../core/services/investment.service';
import { TeamService } from '../../../core/services/team.service';
import { UserService } from '../../../core/services/user.service';
import { StartupResponse, InvestmentResponse, InvitationResponse } from '../../../models';

@Component({
  selector: 'app-home',
  imports: [CommonModule, RouterLink],
  templateUrl: './home.html',
  styleUrl: './home.css'
})
export class HomeComponent implements OnInit {
  // Founder
  myStartups         = signal<StartupResponse[]>([]);
  startupInvestments = signal<InvestmentResponse[]>([]);

  // Investor
  myInvestments = signal<InvestmentResponse[]>([]);

  // CoFounder
  myInvitations = signal<InvitationResponse[]>([]);

  // Name Maps to prevent showing IDs
  startupNames = signal<Map<number, string>>(new Map());
  userNames    = signal<Map<number, string>>(new Map());

  loading = signal(true);

  constructor(
    public authService: AuthService,
    private startupService: StartupService,
    private investmentService: InvestmentService,
    private teamService: TeamService,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    const raw = this.authService.role() ?? '';
    const role = raw.replace('ROLE_', '');
    if (role === 'FOUNDER')   this.loadFounderData();
    if (role === 'INVESTOR')  this.loadInvestorData();
    if (role === 'COFOUNDER') this.loadCofounderData();
    if (role === 'ADMIN')    { this.loading.set(false); }

    // Pre-fetch names for mapping IDs
    this.startupService.getAll({ page: 0, size: 50, sort: 'createdAt,desc' }).subscribe({
      next: env => {
        const map = new Map<number, string>();
        env.data?.content.forEach(s => map.set(s.id, s.name));
        this.startupNames.set(map);
      }
    });

    this.userService.getAllUsers({ page: 0, size: 50, sort: 'id,asc' }).subscribe({
      next: env => {
        const map = new Map<number, string>();
        env.data?.content.forEach(u => map.set(u.userId, u.name || `User ${u.userId}`));
        this.userNames.set(map);
      }
    });
  }

  private loadFounderData(): void {
    this.startupService.getMyStartups({ page: 0, size: 10, sort: 'createdAt,desc' }).subscribe({
      next: env => {
        this.myStartups.set(env.data?.content ?? []);
        this.loading.set(false);
        if (env.data?.content.length) this.loadStartupInvestments(env.data.content[0].id);
      },
      error: () => { this.myStartups.set([]); this.loading.set(false); }
    });
  }

  private loadStartupInvestments(startupId: number): void {
    this.investmentService.getStartupInvestments(startupId, { page: 0, size: 10, sort: 'createdAt,desc' }).subscribe({
      next: env => this.startupInvestments.set(env.data?.content ?? []),
      error: () => this.startupInvestments.set([])
    });
  }

  private loadInvestorData(): void {
    this.investmentService.getMyPortfolio({ page: 0, size: 10, sort: 'createdAt,desc' }).subscribe({
      next: env => { this.myInvestments.set(env.data?.content ?? []); this.loading.set(false); },
      error: () => { this.myInvestments.set([]); this.loading.set(false); }
    });
  }

  private loadCofounderData(): void {
    this.teamService.getMyInvitations({ page: 0, size: 10, sort: 'createdAt,desc' }).subscribe({
      next: env => { this.myInvitations.set(env.data?.content ?? []); this.loading.set(false); },
      error: () => { this.myInvitations.set([]); this.loading.set(false); }
    });
  }

  // ── Computed stats ─────────────────────────────────────────────
  get totalInvested(): number {
    return this.myInvestments().reduce((s, i) => s + i.amount, 0);
  }
  get pendingInvestments(): number {
    return this.myInvestments().filter(i => i.status === 'PENDING').length;
  }
  get approvedInvestments(): number {
    return this.myInvestments().filter(i => i.status === 'APPROVED').length;
  }
  get completedInvestments(): number {
    return this.myInvestments().filter(i => i.status === 'COMPLETED').length;
  }
  get pendingInvitations(): InvitationResponse[] {
    return this.myInvitations().filter(i => i.status === 'PENDING');
  }
  get totalFundingReceived(): number {
    return this.startupInvestments()
      .filter(i => i.status === 'COMPLETED' || i.status === 'APPROVED')
      .reduce((s, i) => s + i.amount, 0);
  }
  get pendingStartupInvestments(): number {
    return this.startupInvestments().filter(i => i.status === 'PENDING').length;
  }

  getStatusClass(status: string): string {
    return status === 'APPROVED'  ? 'badge-success'
         : status === 'COMPLETED' ? 'badge-info'
         : status === 'PENDING'   ? 'badge-warning'
         : status === 'REJECTED'  ? 'badge-danger'
         : 'badge-gray';
  }

  statusLabel(status: string): string {
    const labels: Record<string, string> = {
      PENDING: 'Pending', APPROVED: 'Approved', REJECTED: 'Rejected',
      COMPLETED: 'Completed', PAYMENT_FAILED: 'Failed', STARTUP_CLOSED: 'Closed'
    };
    return labels[status] ?? status;
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(amount);
  }
}
