import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService, UserStatusRequest } from '../services/admin';
import { ClickOutsideDirective } from '../../../shared/directives/click-outside.directive';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule, FormsModule, ClickOutsideDirective],
  templateUrl: './users.html',
  styleUrl: './users.css'
})
export class UsersComponent implements OnInit {
  private adminService = inject(AdminService);

  users: any[] = [];
  page = 0;
  size = 10;
  totalPages = 0;

  searchName = '';
  searchEmail = '';
  roleFilter = '';
  statusFilter = '';

  loading = signal(true);
  openMenuId = signal<number | null>(null);

  confirmActionId = signal<number | null>(null);
  confirmActionType = signal<'SUSPENDED' | 'BANNED' | 'ACTIVE' | null>(null);

  ngOnInit() {
    this.loadUsers();
  }

  loadUsers() {
    this.loading.set(true);
    this.adminService.searchUsers(this.page, this.size, {
      name: this.searchName,
      email: this.searchEmail,
      role: this.roleFilter,
      status: this.statusFilter
    }).subscribe({
      next: (res) => {
        this.users = res.content || [];
        this.totalPages = res.totalPages || 0;
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
    this.loadUsers();
  }

  initiateAction(id: number, type: 'SUSPENDED' | 'BANNED' | 'ACTIVE') {
    this.confirmActionId.set(id);
    this.confirmActionType.set(type);
    this.closeMenu();
  }

  cancelAction() {
    this.confirmActionId.set(null);
    this.confirmActionType.set(null);
  }

  confirmAction(id: number) {
    const type = this.confirmActionType();
    if (!type) return;

    this.adminService.updateUserStatus(id, { status: type }).subscribe({
      next: () => {
        this.loadUsers();
        this.cancelAction();
      },
      error: (err) => alert('Failed to update user status')
    });
  }

  toggleMenu(id: number, event: Event) {
    event.stopPropagation();
    this.openMenuId.update(current => current === id ? null : id);
  }

  closeMenu() {
    this.openMenuId.set(null);
  }

  getRoleColor(role: string): string {
    if (role === 'FOUNDER') return 'badge-purple';
    if (role === 'INVESTOR') return 'badge-blue';
    if (role === 'COFOUNDER') return 'badge-teal';
    return 'badge-gray';
  }
}
