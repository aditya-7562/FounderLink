import { Component, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { StartupService } from '../../core/services/startup.service';
import { InvestmentService } from '../../core/services/investment.service';
import { PaginatedData, StartupResponse, StartupStage } from '../../models';
import { PaginationControlsComponent } from '../../shared/components/pagination-controls/pagination-controls';

@Component({
  selector: 'app-startups',
  imports: [CommonModule, FormsModule, RouterLink, PaginationControlsComponent],
  templateUrl: './startups.html',
  styleUrl: './startups.css'
})
export class StartupsComponent implements OnInit {
  startupPage  = signal<PaginatedData<StartupResponse>>({
    content: [],
    page: 0,
    size: 9,
    totalElements: 0,
    totalPages: 0,
    last: true
  });
  loading     = signal(true);
  errorMsg    = signal('');
  startups    = computed(() => this.startupPage().content);

  // Filters
  selectedStage    = '';
  selectedIndustry = '';
  minFunding       = '';
  maxFunding       = '';
  availableIndustries = signal<string[]>([]);

  // Invest modal
  investModal   = signal<StartupResponse | null>(null);
  investAmount  = 0;
  investing     = signal(false);
  investError   = signal('');
  investSuccess = signal('');

  readonly stages: StartupStage[] = ['IDEA', 'MVP', 'EARLY_TRACTION', 'SCALING'];
  readonly stageLabels: Record<StartupStage, string> = {
    IDEA: 'Idea', MVP: 'MVP', EARLY_TRACTION: 'Early Traction', SCALING: 'Scaling'
  };

  constructor(
    public authService: AuthService,
    private startupService: StartupService,
    private investmentService: InvestmentService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadStartups();
  }

  loadStartups(page = 0): void {
    this.loading.set(true);
    this.errorMsg.set('');
    this.startupService.getAll({ page, size: 9, sort: 'createdAt,desc' }).subscribe({
      next: env => { 
        const startupPage = env.data ?? this.startupPage();
        this.startupPage.set(startupPage);
        const startups = startupPage.content;
        if (this.availableIndustries().length === 0) {
          this.availableIndustries.set([...new Set(startups.map(s => s.industry))].sort());
        }
        this.loading.set(false); 
      },
      error: env => { this.errorMsg.set(env.error ?? 'Failed to load startups.'); this.loading.set(false); }
    });
  }

  applyFilters(page = 0): void {
    this.loading.set(true);
    this.errorMsg.set('');
    const filters: any = {};
    if (this.selectedStage)    filters.stage    = this.selectedStage;
    if (this.selectedIndustry) filters.industry = this.selectedIndustry;
    if (this.minFunding)       filters.minFunding = Number(this.minFunding);
    if (this.maxFunding)       filters.maxFunding = Number(this.maxFunding);
    filters.page = page;
    filters.size = 9;
    filters.sort = 'createdAt,desc';

    this.startupService.search(filters).subscribe({
      next: env => {
        this.startupPage.set(env.data ?? this.startupPage());
        this.loading.set(false);
      },
      error: env => { this.errorMsg.set(env.error ?? 'Search failed.'); this.loading.set(false); }
    });
  }

  clearFilters(): void {
    this.selectedStage = '';
    this.selectedIndustry = '';
    this.minFunding = '';
    this.maxFunding = '';
    this.loadStartups(0);
  }

  get hasFilters(): boolean {
    return !!(this.selectedStage || this.selectedIndustry || this.minFunding || this.maxFunding);
  }

  nextPage(): void {
    const next = this.startupPage().page + 1;
    if (this.hasFilters) {
      this.applyFilters(next);
      return;
    }
    this.loadStartups(next);
  }

  previousPage(): void {
    const previous = Math.max(this.startupPage().page - 1, 0);
    if (this.hasFilters) {
      this.applyFilters(previous);
      return;
    }
    this.loadStartups(previous);
  }


  // ── Invest Modal ──────────────────────────────────────────────
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

  // ── Helpers ───────────────────────────────────────────────────
  private roleIs(r: string): boolean {
    const s = this.authService.role() ?? '';
    return s === r || s === `ROLE_${r}`;
  }
  get isInvestor(): boolean { return this.roleIs('INVESTOR'); }
  get isFounder():  boolean { return this.roleIs('FOUNDER'); }

  stageLabel(stage: string): string {
    return this.stageLabels[stage as StartupStage] ?? stage;
  }

  stageClass(stage: string): string {
    return stage === 'IDEA'           ? 'badge-gray'
         : stage === 'MVP'            ? 'badge-info'
         : stage === 'EARLY_TRACTION' ? 'badge-warning'
         : 'badge-success';
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(amount);
  }
}
