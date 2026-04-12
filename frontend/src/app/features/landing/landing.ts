import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { StartupService } from '../../core/services/startup.service';
import { AuthService } from '../../core/services/auth.service';
import { UserService } from '../../core/services/user.service';
import { StartupResponse, StartupStage } from '../../models';

@Component({
  selector: 'app-landing',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './landing.html',
  styleUrl: './landing.css'
})
export class LandingComponent implements OnInit {
  startups   = signal<StartupResponse[]>([]);
  loading    = signal(true);
  error      = signal('');
  stats      = signal({ 
    founders: 0, investors: 0, cofounders: 0,
    startups: 0, totalFunding: 0
  });
  totalUsers = computed(() => this.stats().founders + this.stats().investors + this.stats().cofounders);
  searchQuery = '';
  stageFilter = '';
  industryFilter = '';

  readonly stages: { value: string; label: string }[] = [
    { value: '', label: 'All Stages' },
    { value: 'IDEA', label: 'Idea' },
    { value: 'MVP', label: 'MVP' },
    { value: 'EARLY_TRACTION', label: 'Early Traction' },
    { value: 'SCALING', label: 'Scaling' },
  ];

  constructor(
    private startupService: StartupService,
    private userService: UserService,
    public  authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadStartups();
    this.loadStats();
  }

  loadStats(): void {
    this.userService.getPublicStats().subscribe({
      next: (data) => this.stats.update(s => ({ ...s, ...data })),
      error: () => console.warn('Failed to load user public stats from backend.')
    });

    this.startupService.getPublicStats().subscribe({
      next: (data) => this.stats.update(s => ({ ...s, startups: data.startups, totalFunding: data.totalFunding })),
      error: () => console.warn('Failed to load startup public stats from backend.')
    });
  }

  loadStartups(): void {
    this.loading.set(true);
    const filters: any = {};
    if (this.stageFilter)    filters.stage    = this.stageFilter;
    if (this.industryFilter) filters.industry = this.industryFilter;

    this.startupService.search(filters).subscribe({
      next:  env => { this.startups.set(env.data?.content ?? []); this.loading.set(false); },
      error: ()  => { this.loading.set(false); }
    });
  }

  get filteredStartups(): StartupResponse[] {
    const q = this.searchQuery.toLowerCase();
    if (!q) return this.startups();
    return this.startups().filter(s =>
      s.name.toLowerCase().includes(q) ||
      (s.industry ?? '').toLowerCase().includes(q) ||
      (s.description ?? '').toLowerCase().includes(q)
    );
  }

  openDetail(s: StartupResponse): void {
    this.router.navigate(['/startup', s.id]);
  }

  goToDashboard(): void {
    this.router.navigate(['/dashboard']);
  }

  stageLabel(stage: StartupStage): string {
    const map: Record<string, string> = {
      IDEA: 'Idea', MVP: 'MVP', EARLY_TRACTION: 'Early Traction', SCALING: 'Scaling'
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
    if (n >= 10_000_000) return `₹${(n / 10_000_000).toFixed(1)}Cr`;
    if (n >= 100_000)    return `₹${(n / 100_000).toFixed(1)}L`;
    return `₹${n.toLocaleString('en-IN')}`;
  }
}
