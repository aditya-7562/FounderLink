import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { StartupService } from '../../../core/services/startup.service';
import { PaginatedData, StartupResponse, StartupRequest, StartupStage } from '../../../models';
import { PaginationControlsComponent } from '../../../shared/components/pagination-controls/pagination-controls';

@Component({
  selector: 'app-my-startup',
  imports: [CommonModule, ReactiveFormsModule, PaginationControlsComponent],
  templateUrl: './my-startup.html',
  styleUrl: './my-startup.css'
})
export class MyStartupComponent implements OnInit {
  startups   = signal<StartupResponse[]>([]);
  startupPage = signal<PaginatedData<StartupResponse>>({
    content: [],
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
    last: true
  });
  loading    = signal(true);
  saving     = signal(false);
  deleting   = signal<number | null>(null);
  errorMsg   = signal('');
  successMsg = signal('');

  showForm   = signal(false);
  editingId  = signal<number | null>(null);

  form: FormGroup;

  readonly stages: StartupStage[] = ['IDEA', 'MVP', 'EARLY_TRACTION', 'SCALING'];
  readonly stageLabels: Record<StartupStage, string> = {
    IDEA: 'Idea', MVP: 'MVP', EARLY_TRACTION: 'Early Traction', SCALING: 'Scaling'
  };

  constructor(
    private fb: FormBuilder,
    public authService: AuthService,
    private startupService: StartupService
  ) {
    this.form = this.fb.group({
      name:             ['', [Validators.required, Validators.minLength(2)]],
      description:      ['', Validators.required],
      industry:         ['', Validators.required],
      problemStatement: ['', Validators.required],
      solution:         ['', Validators.required],
      fundingGoal:      [null, [Validators.required, Validators.min(1000)]],
      stage:            ['', Validators.required]
    });
  }

  ngOnInit(): void { this.loadStartups(); }

  loadStartups(page = 0): void {
    this.loading.set(true);
    this.startupService.getMyStartups({ page, size: 10, sort: 'createdAt,desc' }).subscribe({
      next: env => {
        const startupPage = env.data ?? this.startupPage();
        this.startupPage.set(startupPage);
        this.startups.set(startupPage.content);
        this.loading.set(false);
      },
      error: env => { this.errorMsg.set(env.error ?? 'Failed to load startups.'); this.loading.set(false); }
    });
  }

  openCreate(): void {
    this.form.reset();
    this.editingId.set(null);
    this.showForm.set(true);
    this.errorMsg.set('');
    this.successMsg.set('');
  }

  openEdit(s: StartupResponse): void {
    this.form.patchValue(s);
    this.editingId.set(s.id);
    this.showForm.set(true);
    this.errorMsg.set('');
    this.successMsg.set('');
  }

  cancelForm(): void { this.showForm.set(false); this.editingId.set(null); }

  onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    this.errorMsg.set('');

    const payload: StartupRequest = this.form.value;
    const id = this.editingId();
    const req$ = id ? this.startupService.update(id, payload) : this.startupService.create(payload);

    req$.subscribe({
      next: () => {
        this.saving.set(false);
        this.successMsg.set(id ? 'Startup updated!' : 'Startup created!');
        this.showForm.set(false);
        this.editingId.set(null);
        this.loadStartups();
        setTimeout(() => this.successMsg.set(''), 3000);
      },
      error: env => {
        this.saving.set(false);
        this.errorMsg.set(env.error ?? 'Something went wrong.');
      }
    });
  }

  deleteStartup(id: number): void {
    if (!confirm('Delete this startup? This will also cancel pending investments and team memberships.')) return;
    this.deleting.set(id);
    this.startupService.delete(id).subscribe({
      next: () => {
        this.deleting.set(null);
        this.startups.update(list => list.filter(s => s.id !== id));
        this.successMsg.set('Startup deleted.');
        setTimeout(() => this.successMsg.set(''), 3000);
      },
      error: env => {
        this.deleting.set(null);
        this.errorMsg.set(env.error ?? 'Failed to delete startup.');
      }
    });
  }

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

  get f() { return this.form.controls; }

  nextPage(): void {
    this.loadStartups(this.startupPage().page + 1);
  }

  previousPage(): void {
    this.loadStartups(Math.max(this.startupPage().page - 1, 0));
  }
}
