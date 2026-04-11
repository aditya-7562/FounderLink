import { Component, input, output, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { ThemeService } from '../../../core/services/theme.service';
import { NotificationResponse } from '../../../models';

@Component({
  selector: 'app-navbar',
  imports: [CommonModule, RouterLink],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css'
})
export class NavbarComponent implements OnInit, OnDestroy {
  menuToggle = output<void>();
  pageTitle = input('Dashboard');

  unreadCount = signal(0);
  showNotifPanel = signal(false);
  notifications = signal<NotificationResponse[]>([]);

  private pollInterval: ReturnType<typeof setInterval> | null = null;

  constructor(
    public authService: AuthService,
    public themeService: ThemeService,
    private notificationService: NotificationService,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.loadUnread();
    this.pollInterval = setInterval(() => this.loadUnread(), 30000);
  }

  ngOnDestroy(): void {
    if (this.pollInterval) clearInterval(this.pollInterval);
  }

  loadUnread(): void {
    if (!this.authService.userId()) return;
    this.notificationService.getMyUnreadNotifications({ page: 0, size: 10, sort: 'createdAt,desc' }).subscribe({
      next: env => this.unreadCount.set(env.data?.totalElements ?? 0),
      error: () => { }
    });
  }

  toggleNotifPanel(): void {
    if (!this.showNotifPanel()) this.loadNotifications();
    this.showNotifPanel.update(v => !v);
  }

  loadNotifications(): void {
    this.notificationService.getMyNotifications({ page: 0, size: 10, sort: 'createdAt,desc' }).subscribe({
      next: env => this.notifications.set(env.data?.content ?? []),
      error: () => { }
    });
  }

  markRead(id: number): void {
    this.notificationService.markAsRead(id).subscribe({
      next: env => {
        if (env.data) {
          this.notifications.update(list => list.map(n => n.id === id ? { ...n, read: true } : n));
          this.unreadCount.update(c => Math.max(0, c - 1));
        }
      }
    });
  }

  onNotificationClick(n: NotificationResponse): void {
    if (!n.read) {
      this.markRead(n.id);
    }
    this.showNotifPanel.set(false);

    // Check if it's a message type
    if (n.type.includes('MESSAGE')) {
      const match = n.message.match(/#(\d+)/);
      if (match && match[1]) {
        this.router.navigate(['/dashboard/messages'], { queryParams: { user: match[1] } });
        return;
      }
    }

    // For other notifications, navigate to the notifications page
    this.router.navigate(['/dashboard/notifications']);
  }

  getIcon(type: string): string {
    if (type.includes('INVESTMENT')) return 'investment';
    if (type.includes('TEAM')) return 'team';
    if (type.includes('PAYMENT')) return 'payment';
    if (type.includes('MESSAGE')) return 'message';
    if (type.includes('STARTUP')) return 'startup';
    if (type.includes('REGISTERED')) return 'user';
    if (type.includes('PASSWORD')) return 'lock';
    return 'info';
  }

  getRoleName(): string {
    return this.authService.role()?.replace('ROLE_', '') ?? '';
  }
}
