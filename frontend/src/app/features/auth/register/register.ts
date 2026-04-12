import { Component, OnInit, signal, DestroyRef, inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import { ThemeService } from '../../../core/services/theme.service';

@Component({
  selector: 'app-register',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.css'
})
export class RegisterComponent implements OnInit {
  form: FormGroup;
  loading    = signal(false);
  errorMsg   = signal('');
  successMsg = signal('');
  showPassword = false;
  isRoleLocked = false;
  stats = signal({ founders: 0, investors: 0, cofounders: 0 });

  readonly roles = [
    { value: 'FOUNDER',   icon: 'founder', label: 'Founder',    desc: 'Build a startup' },
    { value: 'INVESTOR',  icon: 'investor', label: 'Investor',   desc: 'Fund startups' },
    { value: 'COFOUNDER', icon: 'cofounder', label: 'Co-Founder', desc: 'Join a team' }
  ];

  readonly rolePreviews = [
    { role: 'Founder',    icon: 'founder', desc: 'Create and manage your startup' },
    { role: 'Investor',   icon: 'investor', desc: 'Discover and fund opportunities' },
    { role: 'Co-Founder', icon: 'cofounder', desc: 'Join teams and build together' },
  ];

  get selectedRole(): string {
    const v = this.role.value as string;
    if (!v) return '';
    return v === 'FOUNDER' ? 'Founder' : v === 'INVESTOR' ? 'Investor' : 'Co-Founder';
  }

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private userService: UserService,
    public  themeService: ThemeService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.form = this.fb.group({
      name:     ['', [Validators.required, Validators.minLength(2)]],
      email:    ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      role:     ['', Validators.required]
    });
  }

  private destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.route.queryParams.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(params => {
      const qRole = params['role'];
      if (qRole && ['FOUNDER', 'INVESTOR', 'COFOUNDER'].includes(qRole)) {
        this.form.patchValue({ role: qRole });
        this.isRoleLocked = true;
      }
    });

    this.userService.getPublicStats().subscribe({
      next: (data) => this.stats.set(data),
      error: () => console.warn('Failed to load public stats')
    });
  }

  get name()     { return this.form.get('name')!; }
  get email()    { return this.form.get('email')!; }
  get password() { return this.form.get('password')!; }
  get role()     { return this.form.get('role')!; }

  onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading.set(true);
    this.errorMsg.set('');

    this.authService.register(this.form.value).subscribe({
      next: () => {
        this.loading.set(false);
        this.successMsg.set('Account created successfully! Redirecting to login...');
        setTimeout(() => this.router.navigate(['/auth/login']), 1800);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMsg.set(err.error?.message || 'Registration failed. Please try again.');
      }
    });
  }

  isDirty(): boolean {
    return this.form.dirty && !this.successMsg();
  }
}
