import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { StartupService } from '../../../core/services/startup.service';
import { WalletService } from '../../../core/services/wallet.service';
import { AuthService } from '../../../core/services/auth.service';
import { ThemeService } from '../../../core/services/theme.service';
import { InvestmentService } from '../../../core/services/investment.service';
import { StartupResponse, StartupStage, WalletResponse } from '../../../models';
import { computed } from '@angular/core';

@Component({
  selector: 'app-startup-detail',
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './startup-detail.html',
  styleUrl: './startup-detail.css'
})
export class StartupDetailComponent implements OnInit {
  startup = signal<StartupResponse | null>(null);
  wallet  = signal<WalletResponse | null>(null);
  loading = signal(true);
  error   = signal('');

  raisedAmount = computed(() => this.wallet()?.balance ?? 0);

  fundingPercentage = computed(() => {
    const goal = this.startup()?.fundingGoal ?? 0;
    if (goal <= 0) return 0;
    const percent = (this.raisedAmount() / goal) * 100;
    return Math.min(Math.round(percent), 100);
  });

  investModal   = signal<StartupResponse | null>(null);
  investAmount  = 0;
  investing     = signal(false);
  investError   = signal('');
  investSuccess = signal('');

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private startupService: StartupService,
    private walletService: WalletService,
    private investmentService: InvestmentService,
    public  authService: AuthService,
    public  themeService: ThemeService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.startupService.getDetails(id).subscribe({
      next:  env => { 
        this.startup.set(env.data); 
        this.loading.set(false);
        this.fetchWallet(id);
      },
      error: ()  => { this.error.set('Startup not found.'); this.loading.set(false); }
    });
  }

  private fetchWallet(id: number): void {
    this.walletService.getWallet(id).subscribe({
      next:  env => this.wallet.set(env.data),
      error: ()  => this.wallet.set(null) // Not found or error -> 0 balance
    });
  }

  openInvestModal(startup: StartupResponse): void {
    this.investModal.set(startup);
    this.investAmount = 0;
    this.investError.set('');
    this.investSuccess.set('');
  }

  closeInvestModal(): void {
    this.investModal.set(null);
    this.investAmount = 0;
    this.investError.set('');
    this.investSuccess.set('');
  }

  submitInvestment(): void {
    const startup = this.investModal();
    if (!startup) return;
    if (!this.investAmount || this.investAmount < 1000) {
      this.investError.set('Minimum investment is ₹1,000.');
      return;
    }

    this.investing.set(true);
    this.investError.set('');

    this.investmentService.create({ startupId: startup.id, amount: this.investAmount }).subscribe({
      next: () => {
        this.investing.set(false);
        this.investSuccess.set('Investment submitted successfully! Awaiting founder approval.');
        setTimeout(() => this.closeInvestModal(), 2500);
      },
      error: env => {
        this.investing.set(false);
        this.investError.set(env.error ?? 'Failed to submit investment.');
      }
    });
  }

  messageFounder(founderId: number): void {
    this.router.navigate(['/dashboard/messages'], { queryParams: { user: founderId } });
  }

  goBack(): void { 
    if (this.authService.isLoggedIn()) {
      this.router.navigate(['/dashboard/startups']);
    } else {
      this.router.navigate(['/']); 
    }
  }

  private roleIs(r: string): boolean {
    const s = this.authService.role() ?? '';
    return s === r || s === `ROLE_${r}`;
  }

  get isInvestor(): boolean { return this.roleIs('INVESTOR'); }

  stageLabel(stage: StartupStage): string {
    const map: Record<string, string> = {
      IDEA: 'Idea Stage', MVP: 'MVP', EARLY_TRACTION: 'Early Traction', SCALING: 'Scaling'
    };
    return map[stage] ?? stage;
  }

  stageClass(stage: StartupStage): string {
    return stage === 'IDEA'           ? 'stage-idea'
         : stage === 'MVP'            ? 'stage-mvp'
         : stage === 'EARLY_TRACTION' ? 'stage-traction'
         : 'stage-scaling';
  }

  formatCurrency(n: number): string {
    if (!n) return '₹0';
    if (n >= 10_000_000) return `₹${(n / 10_000_000).toFixed(2)}Cr`;
    if (n >= 100_000)    return `₹${(n / 100_000).toFixed(2)}L`;
    return `₹${n.toLocaleString('en-IN')}`;
  }
}
