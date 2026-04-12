import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { StartupService } from '../../../core/services/startup.service';
import { InvestmentService } from '../../../core/services/investment.service';
import { TeamService } from '../../../core/services/team.service';
import { UserService } from '../../../core/services/user.service';
import { ApiEnvelope, UserResponse, StartupResponse, InvestmentResponse, InvitationResponse } from '../../../models';

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

  totalPendingActions = signal(0);
  totalFundingAmount  = signal(0);
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
        const map = new Map(this.userNames());
        env.data?.content.forEach(u => map.set(u.userId, u.name || `User ${u.userId}`));
        this.userNames.set(map);
      }
    });

    // Ensure current user's name is loaded
    const currentId = this.authService.userId();
    if (currentId) {
      this.userService.getUser(currentId).subscribe({
        next: env => {
          if (env.data) {
            const map = new Map(this.userNames());
            map.set(env.data.userId, env.data.name || `User ${env.data.userId}`);
            this.userNames.set(map);
          }
        }
      });
    }
  }

  private loadFounderData(): void {
    this.startupService.getMyStartups({ page: 0, size: 10, sort: 'createdAt,desc' }).subscribe({
      next: env => {
        const startups = env.data?.content ?? [];
        this.myStartups.set(startups);
        this.loading.set(false);
        if (startups.length) {
          this.loadStartupInvestments(startups[0].id);
          this.loadGlobalFounderStats(startups.map(s => s.id));
        }
      },
      error: () => { this.myStartups.set([]); this.loading.set(false); }
    });
  }

  private loadGlobalFounderStats(startupIds: number[]): void {
    let globalPending = 0;
    let globalFunding = 0;
    let processed = 0;

    if (startupIds.length === 0) {
      this.totalPendingActions.set(0);
      this.totalFundingAmount.set(0);
      return;
    }

    startupIds.forEach(id => {
      this.investmentService.getStartupInvestments(id, { page: 0, size: 100 }).subscribe({
        next: env => {
          const content = env.data?.content ?? [];
          
          // Count global pending
          globalPending += content.filter(i => i.status === 'PENDING').length;
          
          // Sum global funding (Approved or Completed)
          globalFunding += content
            .filter(i => i.status === 'APPROVED' || i.status === 'COMPLETED')
            .reduce((s, i) => s + i.amount, 0);

          processed++;
          if (processed === startupIds.length) {
            this.totalPendingActions.set(globalPending);
            this.totalFundingAmount.set(globalFunding);
          }
        },
        error: () => {
          processed++;
          if (processed === startupIds.length) {
            this.totalPendingActions.set(globalPending);
            this.totalFundingAmount.set(globalFunding);
          }
        }
      });
    });
  }

  private loadStartupInvestments(startupId: number): void {
    this.investmentService.getStartupInvestments(startupId, { page: 0, size: 10, sort: 'createdAt,desc' }).subscribe({
      next: env => {
        const invs = env.data?.content ?? [];
        this.startupInvestments.set(invs);
        this.fetchMissingUserNames(invs.map(i => i.investorId));
      },
      error: () => this.startupInvestments.set([])
    });
  }

  private loadInvestorData(): void {
    this.investmentService.getMyPortfolio({ page: 0, size: 10, sort: 'createdAt,desc' }).subscribe({
      next: env => {
        const invs = env.data?.content ?? [];
        this.myInvestments.set(invs);
        this.fetchMissingStartupNames(invs.map(i => i.startupId));
        this.loading.set(false);
      },
      error: () => { this.myInvestments.set([]); this.loading.set(false); }
    });
  }

  private loadCofounderData(): void {
    this.teamService.getMyInvitations({ page: 0, size: 10, sort: 'createdAt,desc' }).subscribe({
      next: env => {
        const invs = env.data?.content ?? [];
        this.myInvitations.set(invs);
        this.fetchMissingStartupNames(invs.map(i => i.startupId));
        this.loading.set(false);
      },
      error: () => { this.myInvitations.set([]); this.loading.set(false); }
    });
  }

  private fetchMissingUserNames(userIds: number[]): void {
    const currentMap = this.userNames();
    const missingIds = Array.from(new Set(userIds)).filter(id => id > 0 && !currentMap.has(id));

    missingIds.forEach(id => {
      this.userService.getUser(id).subscribe({
        next: (env: ApiEnvelope<UserResponse>) => {
          if (env.data) {
            const nextMap = new Map(this.userNames());
            nextMap.set(env.data.userId, env.data.name || `User ${env.data.userId}`);
            this.userNames.set(nextMap);
          }
        }
      });
    });
  }

  private fetchMissingStartupNames(startupIds: number[]): void {
    const currentMap = this.startupNames();
    const missingIds = Array.from(new Set(startupIds)).filter(id => id > 0 && !currentMap.has(id));

    missingIds.forEach(id => {
      this.startupService.getDetails(id).subscribe({
        next: (env: ApiEnvelope<StartupResponse>) => {
          if (env.data) {
            const nextMap = new Map(this.startupNames());
            nextMap.set(env.data.id, env.data.name);
            this.startupNames.set(nextMap);
          }
        }
      });
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
