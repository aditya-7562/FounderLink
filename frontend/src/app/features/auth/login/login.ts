import { Component, signal, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';

@Component({
  selector: 'app-login',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class LoginComponent implements OnInit {
  form: FormGroup;
  loading = signal(false);
  errorMsg = signal('');
  showPassword = false;
  stats = signal({ founders: 0, investors: 0, cofounders: 0 });

  readonly features = [
    { icon: 'founder', title: 'Launch Your Startup',   desc: 'Create your profile and attract top talent and investors' },
    { icon: 'investor', title: 'Connect with Investors', desc: 'Get funded by verified investors who believe in your vision' },
    { icon: 'cofounder', title: 'Build Your Dream Team',  desc: 'Find co-founders and team members with the right skills' },
  ];

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private userService: UserService,
    private router: Router
  ) {
    this.form = this.fb.group({
      email:    ['', [Validators.required, Validators.email]],
      password: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    this.userService.getPublicStats().subscribe({
      next: (data) => this.stats.set(data),
      error: () => console.warn('Failed to load public stats')
    });
  }

  get email()    { return this.form.get('email')!; }
  get password() { return this.form.get('password')!; }

  onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading.set(true);
    this.errorMsg.set('');

    this.authService.login(this.form.value).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigateByUrl(this.authService.getHomeRoute());
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMsg.set(err.error?.message || 'Invalid email or password. Please try again.');
      }
    });
  }
}
