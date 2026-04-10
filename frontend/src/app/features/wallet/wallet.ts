import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';
import { StartupService } from '../../core/services/startup.service';
import { WalletService } from '../../core/services/wallet.service';
import { PaginatedData, StartupResponse, WalletResponse } from '../../models';
import { PaginationControlsComponent } from '../../shared/components/pagination-controls/pagination-controls';

@Component({
  selector: 'app-wallet',
  imports: [CommonModule, PaginationControlsComponent],
  templateUrl: './wallet.html',
  styleUrl: './wallet.css'
})
export class WalletComponent implements OnInit {
  startups          = signal<StartupResponse[]>([]);
  startupPage       = signal<PaginatedData<StartupResponse>>({
    content: [],
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
    last: true
  });
  selectedStartupId = signal<number | null>(null);
  wallet            = signal<WalletResponse | null>(null);
  loading           = signal(true);
  walletLoading     = signal(false);
  errorMsg          = signal('');

  selectedStartup = computed(() => this.startups().find(s => s.id === this.selectedStartupId()));

  constructor(
    public authService: AuthService,
    private startupService: StartupService,
    private walletService: WalletService
  ) {}

  ngOnInit(): void {
    this.loadFounderStartups();
  }

  loadFounderStartups(page = 0): void {
    this.loading.set(true);
    this.startupService.getMyStartups({ page, size: 10, sort: 'createdAt,desc' }).subscribe({
      next: env => {
        const startupPage = env.data ?? this.startupPage();
        this.startupPage.set(startupPage);
        const list = startupPage.content;
        this.startups.set(list);
        if (list.length > 0) {
          const current = this.selectedStartupId();
          const selected = list.find(s => s.id === current) ?? list[0];
          this.selectedStartupId.set(selected.id);
          this.loadWallet(selected.id);
        }
        this.loading.set(false);
      },
      error: () => {
        this.errorMsg.set('Failed to load startups.');
        this.loading.set(false);
      }
    });
  }

  onStartupChange(startupId: number): void {
    this.selectedStartupId.set(startupId);
    this.loadWallet(startupId);
  }

  loadWallet(startupId: number): void {
    this.walletLoading.set(true);
    this.wallet.set(null);
    this.walletService.getWallet(startupId).subscribe({
      next: env => {
        this.wallet.set(env.data);
        this.walletLoading.set(false);
      },
      error: () => {
        // Wallet doesn't exist yet — it's created on first payment
        this.wallet.set(null);
        this.walletLoading.set(false);
      }
    });
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', minimumFractionDigits: 2 }).format(amount);
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleDateString('en-IN', { year: 'numeric', month: 'short', day: 'numeric' });
  }

  nextPage(): void {
    this.loadFounderStartups(this.startupPage().page + 1);
  }

  previousPage(): void {
    this.loadFounderStartups(Math.max(this.startupPage().page - 1, 0));
  }
}
