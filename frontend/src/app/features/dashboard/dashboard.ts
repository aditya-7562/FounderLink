import { Component, signal, computed } from '@angular/core';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { SidebarComponent } from '../../shared/components/sidebar/sidebar';
import { NavbarComponent } from '../../shared/components/navbar/navbar';
import { AuthService } from '../../core/services/auth.service';
import { filter } from 'rxjs';

@Component({
  selector: 'app-dashboard',
  imports: [CommonModule, RouterOutlet, SidebarComponent, NavbarComponent],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class DashboardComponent {
  sidebarOpen = signal(true);
  pageTitle   = signal('Dashboard');

  private readonly titleMap: Record<string, string> = {
    '/admin/dashboard':       'Admin Portal',
    '/admin/users':           'Users Management',
    '/admin/startups':        'Startups Moderation',
    '/dashboard':             'Dashboard',
    '/dashboard/startups':    'Startups',
    '/dashboard/my-startup':  'My Startup',
    '/dashboard/team':        'Team',
    '/dashboard/invitations': 'Invitations',
    '/dashboard/investments': 'Investments',
    '/dashboard/portfolio':   'My Portfolio',
    '/dashboard/payments':    'Payments',
    '/dashboard/wallet':      'Wallet',
    '/dashboard/messages':    'Messages',
    '/dashboard/notifications':'Notifications',
    '/dashboard/profile':     'Profile',
  };

  constructor(public authService: AuthService, private router: Router) {
    this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe((e: NavigationEnd) => {
        const path = e.urlAfterRedirects.split('?')[0];
        this.pageTitle.set(this.titleMap[path] ?? 'Dashboard');
      });
  }

  toggleSidebar(): void {
    this.sidebarOpen.update(v => !v);
  }
}
