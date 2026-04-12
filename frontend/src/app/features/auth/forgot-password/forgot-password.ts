import { Component, signal } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';
import { ThemeService } from '../../../core/services/theme.service';

@Component({
  selector: 'app-forgot-password',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './forgot-password.html',
  styleUrl: './forgot-password.css'
})
export class ForgotPasswordComponent {
  step = signal<'request' | 'reset'>('request');
  loading = signal(false);
  errorMsg = signal('');
  successMsg = signal('');
  submittedEmail = signal('');
  showPassword = false;

  requestForm: FormGroup;
  resetForm: FormGroup;

  constructor(
    private fb: FormBuilder, 
    private authService: AuthService,
    public themeService: ThemeService
  ) {
    this.requestForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });

    this.resetForm = this.fb.group({
      email:       ['', [Validators.required, Validators.email]],
      pin:         ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6)]],
      newPassword: ['', [Validators.required, Validators.minLength(6)]]
    });
  }

  get reqEmail()    { return this.requestForm.get('email')!; }
  get resEmail()    { return this.resetForm.get('email')!; }
  get pin()         { return this.resetForm.get('pin')!; }
  get newPassword() { return this.resetForm.get('newPassword')!; }

  sendPin(): void {
    if (this.requestForm.invalid) { this.requestForm.markAllAsTouched(); return; }

    this.loading.set(true);
    this.errorMsg.set('');

    this.authService.forgotPassword({ email: this.reqEmail.value }).subscribe({
      next: () => {
        this.loading.set(false);
        this.submittedEmail.set(this.reqEmail.value);
        this.resetForm.patchValue({ email: this.reqEmail.value });
        this.step.set('reset');
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMsg.set(err.error?.message || 'Email not found.');
      }
    });
  }

  resetPassword(): void {
    if (this.resetForm.invalid) { this.resetForm.markAllAsTouched(); return; }

    this.loading.set(true);
    this.errorMsg.set('');

    this.authService.resetPassword(this.resetForm.value).subscribe({
      next: () => {
        this.loading.set(false);
        this.successMsg.set('Password reset successfully! You can now sign in.');
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMsg.set(err.error?.message || 'Invalid or expired PIN.');
      }
    });
  }
}
