import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { unsavedChangesGuard } from './core/guards/unsaved-changes.guard';

export const routes: Routes = [

  // ── Public: Landing page ───────────────────────────────────────────────────
  {
    path: '',
    loadComponent: () => import('./features/landing/landing').then(m => m.LandingComponent)
  },

  // ── Public: Individual startup detail ─────────────────────────────────────
  {
    path: 'startup/:id',
    loadComponent: () => import('./features/landing/startup-detail/startup-detail').then(m => m.StartupDetailComponent)
  },

  // ── Auth (public) ──────────────────────────────────────────────────────────
  {
    path: 'auth',
    children: [
      { path: 'login',           loadComponent: () => import('./features/auth/login/login').then(m => m.LoginComponent) },
      { path: 'register',        canDeactivate: [unsavedChangesGuard], loadComponent: () => import('./features/auth/register/register').then(m => m.RegisterComponent) },
      { path: 'forgot-password', loadComponent: () => import('./features/auth/forgot-password/forgot-password').then(m => m.ForgotPasswordComponent) },
      { path: '', redirectTo: 'login', pathMatch: 'full' }
    ]
  },

  // ── Protected (dashboard shell) ────────────────────────────────────────────
  {
    path: 'dashboard',
    canActivate: [authGuard, roleGuard(['FOUNDER', 'INVESTOR', 'COFOUNDER'])],
    loadComponent: () => import('./features/dashboard/dashboard').then(m => m.DashboardComponent),
    children: [
      { path: '', loadComponent: () => import('./features/dashboard/home/home').then(m => m.HomeComponent) },

      // Startups
      { path: 'startups',    loadComponent: () => import('./features/startups/startups').then(m => m.StartupsComponent) },
      { path: 'my-startup',  loadComponent: () => import('./features/startups/my-startup/my-startup').then(m => m.MyStartupComponent) },

      // Team
      { path: 'team',        loadComponent: () => import('./features/team/team').then(m => m.TeamComponent) },
      { path: 'invitations', loadComponent: () => import('./features/team/invitations/invitations').then(m => m.InvitationsComponent) },

      // Investments
      { path: 'investments', loadComponent: () => import('./features/investments/investments').then(m => m.InvestmentsComponent) },
      { path: 'portfolio',   loadComponent: () => import('./features/investments/portfolio/portfolio').then(m => m.PortfolioComponent) },

      // Payments & Wallet
      { path: 'payments',    loadComponent: () => import('./features/payments/payments').then(m => m.PaymentsComponent) },
      { path: 'wallet',      loadComponent: () => import('./features/wallet/wallet').then(m => m.WalletComponent) },

      // Messages & Notifications
      { path: 'messages',       loadComponent: () => import('./features/messages/messages').then(m => m.MessagesComponent) },
      { path: 'notifications',  loadComponent: () => import('./features/notifications/notifications').then(m => m.NotificationsComponent) },

      // Profile
      { path: 'profile',     loadComponent: () => import('./features/profile/profile').then(m => m.ProfileComponent) },
    ]
  },

  // ── Protected (Admin shell) ────────────────────────────────────────────────
  {
    path: 'admin',
    canActivate: [authGuard, roleGuard(['ADMIN'])],
    loadComponent: () => import('./features/dashboard/dashboard').then(m => m.DashboardComponent),
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', loadComponent: () => import('./features/admin/dashboard/dashboard').then(m => m.DashboardComponent) },
      { path: 'users',     loadComponent: () => import('./features/admin/users/users').then(m => m.UsersComponent) },
      { path: 'startups',  loadComponent: () => import('./features/admin/startups/startups').then(m => m.StartupsComponent) }
    ]
  },

  // ── Fallback ───────────────────────────────────────────────────────────────
  { path: '**', redirectTo: '' }
];
