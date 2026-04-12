import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminService, ModerationRequest } from '../services/admin';
import { UiNotifierService } from '../../../core/services/ui-notifier.service';
import { ClickOutsideDirective } from '../../../shared/directives/click-outside.directive';

@Component({
  selector: 'app-admin-startups',
  standalone: true,
  imports: [CommonModule, ClickOutsideDirective],
  templateUrl: './startups.html',
  styleUrl: './startups.css'
})
export class StartupsComponent implements OnInit {
  private adminService = inject(AdminService);
  private uiNotifier = inject(UiNotifierService);

  startups: any[] = [];
  page = 0;
  size = 10;
  totalPages = 0;

  loading = signal(true);
  openMenuId = signal<number | null>(null);

  confirmActionId = signal<number | null>(null);
  confirmActionType = signal<'APPROVE' | 'FLAG' | 'REJECT' | 'DELETE' | null>(null);
  confirmReason = signal<string>('');

  ngOnInit() {
    this.loadStartups();
  }

  loadStartups() {
    this.loading.set(true);
    this.adminService.getStartups(this.page, this.size).subscribe({
      next: (res) => {
        // Handle varying pagination shapes
        const pageData = res.data || res;
        this.startups = pageData.content || [];
        this.totalPages = pageData.totalPages || 0;
        this.loading.set(false);
      },
      error: (err) => {
        console.error(err);
        this.loading.set(false);
      }
    });
  }

  changePage(newPage: number) {
    this.page = newPage;
    this.loadStartups();
  }

  initiateAction(id: number, type: 'APPROVE' | 'FLAG' | 'REJECT' | 'DELETE') {
    this.confirmActionId.set(id);
    this.confirmActionType.set(type);
    this.confirmReason.set('');
    this.closeMenu();
  }

  cancelAction() {
    this.confirmActionId.set(null);
    this.confirmActionType.set(null);
    this.confirmReason.set('');
  }

  setConfirmReason(event: Event) {
    this.confirmReason.set((event.target as HTMLInputElement).value);
  }

  confirmAction(id: number) {
    const type = this.confirmActionType();
    const reason = this.confirmReason();

    if (type === 'DELETE') {
      this.adminService.deleteStartup(id).subscribe({
        next: () => {
           this.loadStartups();
           this.cancelAction();
        },
        error: (err) => console.error('Failed to delete startup', err)
      });
    } else if (type === 'APPROVE') {
      this.adminService.updateStartupModeration(id, { status: 'APPROVED' }).subscribe({
        next: () => {
          this.loadStartups();
          this.cancelAction();
        },
        error: (err) => console.error('Failed to approve startup', err)
      });
    } else if (type === 'FLAG' || type === 'REJECT') {
      if (!reason.trim()) {
        this.uiNotifier.warning('Reason is required.');
        return;
      }
      const mappedStatus = type === 'FLAG' ? 'FLAGGED' : 'REJECTED';
      this.adminService.updateStartupModeration(id, { status: mappedStatus, reason }).subscribe({
        next: () => {
          this.loadStartups();
          this.cancelAction();
        },
        error: (err) => console.error('Failed to update startup status', err)
      });
    }
  }

  toggleMenu(id: number, event: Event) {
    event.stopPropagation();
    this.openMenuId.update(current => current === id ? null : id);
  }

  closeMenu() {
    this.openMenuId.set(null);
  }

  getDisplayStatus(status: string): string {
    if (!status) return 'Approved';
    if (status === 'PENDING_REVIEW') return 'Pending';
    return status.charAt(0).toUpperCase() + status.slice(1).toLowerCase();
  }
}
