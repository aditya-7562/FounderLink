import { Component, computed, input, output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { TitleCasePipe } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';

interface NavItem {
  label: string;
  route: string;
  icon: string;
  roles: string[];
}

@Component({
  selector: 'app-sidebar',
  imports: [TitleCasePipe, RouterLink, RouterLinkActive],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css'
})
export class SidebarComponent {
  collapsed = input(false);

  readonly navItems: NavItem[] = [
    { label: 'Admin Dashboard', route: '/admin/dashboard',     icon: 'grid',        roles: ['ADMIN'] },
    { label: 'Users',           route: '/admin/users',         icon: 'users',       roles: ['ADMIN'] },
    { label: 'Startups (Admin)',route: '/admin/startups',      icon: 'rocket',      roles: ['ADMIN'] },
    { label: 'Dashboard',       route: '/dashboard',           icon: 'grid',        roles: ['FOUNDER', 'INVESTOR', 'COFOUNDER'] },
    { label: 'Startups',        route: '/dashboard/startups',  icon: 'rocket',      roles: ['FOUNDER', 'INVESTOR', 'COFOUNDER'] },
    { label: 'My Startup',   route: '/dashboard/my-startup',   icon: 'briefcase',   roles: ['FOUNDER'] },
    { label: 'Team',         route: '/dashboard/team',         icon: 'users',       roles: ['FOUNDER', 'COFOUNDER'] },
    { label: 'Invitations',  route: '/dashboard/invitations',  icon: 'mail',        roles: ['COFOUNDER'] },
    { label: 'Investments',  route: '/dashboard/investments',  icon: 'trending-up', roles: ['FOUNDER'] },
    { label: 'Portfolio',    route: '/dashboard/portfolio',    icon: 'pie-chart',   roles: ['INVESTOR'] },
    { label: 'Payments',     route: '/dashboard/payments',     icon: 'credit-card', roles: ['INVESTOR'] },
    { label: 'Wallet',       route: '/dashboard/wallet',       icon: 'wallet',      roles: ['FOUNDER'] },
    { label: 'Messages',     route: '/dashboard/messages',     icon: 'message',     roles: ['FOUNDER', 'INVESTOR', 'COFOUNDER'] },
    { label: 'Profile',      route: '/dashboard/profile',      icon: 'user',        roles: ['FOUNDER', 'INVESTOR', 'COFOUNDER'] },
  ];

  constructor(public authService: AuthService) {}

  readonly visibleItems = computed(() => {
    const rawRole = this.authService.role();
    if (!rawRole) return this.navItems;
    // Strip ROLE_ prefix (stored role is e.g. ROLE_FOUNDER, navItems use FOUNDER)
    const role = rawRole.replace('ROLE_', '');
    return this.navItems.filter(item => item.roles.includes(role));
  });
}
