import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Role guard factory.
 * Pass allowed roles WITHOUT the ROLE_ prefix (e.g. ['FOUNDER', 'INVESTOR']).
 * The stored JWT role has the ROLE_ prefix, so we strip it before comparing.
 */
export const roleGuard = (allowedRoles: string[]): CanActivateFn => () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const rawRole = auth.role();
  if (!rawRole) {
    router.navigate(['/auth/login']);
    return false;
  }

  // Strip ROLE_ prefix for comparison
  const role = rawRole.replace('ROLE_', '');
  if (allowedRoles.includes(role)) return true;

  router.navigateByUrl(auth.getHomeRoute(rawRole));
  return false;
};
