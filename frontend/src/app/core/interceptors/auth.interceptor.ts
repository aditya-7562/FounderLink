import { HttpInterceptorFn, HttpErrorResponse, HttpRequest, HttpHandlerFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { BehaviorSubject, catchError, filter, switchMap, take, throwError, Observable } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';
import { AuthResponse } from '../../models';

// Shared state to prevent concurrent refresh races
let isRefreshing = false;
const refreshSubject = new BehaviorSubject<AuthResponse | null>(null);

/** For testing purposes only */
export function resetAuthInterceptor(): void {
  isRefreshing = false;
  refreshSubject.next(null);
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const token = authService.token();

  // Endpoints where we NEVER attach a token (auth routes only)
  const isPublicAuthEndpoint =
    req.url.includes('/auth/login') ||
    req.url.includes('/auth/register') ||
    req.url.includes('/auth/forgot-password') ||
    req.url.includes('/auth/reset-password') ||
    req.url.includes('/auth/refresh');

  // Endpoints where a 401 should NOT trigger a redirect to /auth/login
  // (e.g. public platform features that don't absolutely require being logged in)
  const ignore401Redirect =
    isPublicAuthEndpoint ||
    req.url.includes('/startup') ||
    req.url.includes('/wallets') ||
    req.url.includes('/users/public/stats');

  const authReq = token && !isPublicAuthEndpoint
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !ignore401Redirect) {
        return handle401(req, next, authService, router);
      }
      return throwError(() => error);
    })
  );
};

function handle401(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
  authService: AuthService,
  router: Router
): Observable<any> {
  if (isRefreshing) {
    // Another refresh is already in-flight — wait for it to complete
    return refreshSubject.pipe(
      filter(res => res !== null),
      take(1),
      switchMap(res => {
        const retryReq = req.clone({
          setHeaders: { Authorization: `Bearer ${res!.token}` }
        });
        return next(retryReq);
      })
    );
  }

  isRefreshing = true;
  refreshSubject.next(null);

  return authService.refresh().pipe(
    switchMap(res => {
      isRefreshing = false;
      refreshSubject.next(res);
      const retryReq = req.clone({
        setHeaders: { Authorization: `Bearer ${res.token}` }
      });
      return next(retryReq);
    }),
    catchError(err => {
      isRefreshing = false;
      refreshSubject.next(null);
      router.navigate(['/auth/login']);
      return throwError(() => err);
    })
  );
}
