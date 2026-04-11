import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  ForgotPasswordRequest,
  ResetPasswordRequest
} from '../../models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = environment.apiUrl;

  private _token  = signal<string | null>(localStorage.getItem('token'));
  private _userId = signal<number | null>(
    localStorage.getItem('userId') ? Number(localStorage.getItem('userId')) : null
  );
  private _role  = signal<string | null>(localStorage.getItem('role'));
  private _email = signal<string | null>(localStorage.getItem('email'));

  readonly token      = this._token.asReadonly();
  readonly userId     = this._userId.asReadonly();
  readonly role       = this._role.asReadonly();
  readonly email      = this._email.asReadonly();
  readonly isLoggedIn = computed(() => !!this._token());

  constructor(private http: HttpClient, private router: Router) {}

  register(request: RegisterRequest): Observable<any> {
    return this.http.post(`${this.api}/auth/register`, request);
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.api}/auth/login`, request, {
      withCredentials: true
    }).pipe(
      tap(res => this.storeSession(res))
    );
  }

  refresh(): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.api}/auth/refresh`, {}, {
      withCredentials: true
    }).pipe(
      tap(res => this.storeSession(res))
    );
  }

  logout(): void {
    this.http.post(`${this.api}/auth/logout`, {}, { withCredentials: true })
      .subscribe({
        complete: () => this.clearSession(),
        error: ()    => this.clearSession()   // ← fixed: clear session even on network error
      });
  }

  forgotPassword(request: ForgotPasswordRequest): Observable<any> {
    return this.http.post(`${this.api}/auth/forgot-password`, request);
  }

  resetPassword(request: ResetPasswordRequest): Observable<any> {
    return this.http.post(`${this.api}/auth/reset-password`, request);
  }

  getHomeRoute(role = this._role()): string {
    const normalizedRole = role?.replace('ROLE_', '');
    return normalizedRole === 'ADMIN' ? '/admin/dashboard' : '/dashboard';
  }

  private storeSession(res: AuthResponse): void {
    localStorage.setItem('token',  res.token);
    localStorage.setItem('userId', String(res.userId));
    localStorage.setItem('role',   res.role);
    localStorage.setItem('email',  res.email);
    this._token.set(res.token);
    this._userId.set(res.userId);
    this._role.set(res.role);
    this._email.set(res.email);
  }

  clearSession(): void {
    localStorage.clear();
    this._token.set(null);
    this._userId.set(null);
    this._role.set(null);
    this._email.set(null);
    this.router.navigate(['/auth/login']);
  }
}
