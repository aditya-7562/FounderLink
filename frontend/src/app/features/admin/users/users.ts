import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService, UserStatusRequest } from '../services/admin';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="animate-in fade-in slide-in-from-bottom-4 duration-500">
      <div class="mb-8 border-b border-neutral-800 pb-4 flex justify-between items-end">
        <div>
          <h2 class="text-3xl font-bold tracking-tight text-white">Users Management</h2>
          <p class="text-neutral-400 mt-2">View and moderate user accounts across the platform.</p>
        </div>
      </div>

      <!-- Controls -->
      <div class="flex flex-wrap gap-4 mb-6">
        <input type="text" [(ngModel)]="searchName" placeholder="Search by name..." 
               class="bg-neutral-950 border border-neutral-800 rounded-md px-4 py-2 text-white focus:outline-none focus:border-blue-500">
        <input type="text" [(ngModel)]="searchEmail" placeholder="Search by email..." 
               class="bg-neutral-950 border border-neutral-800 rounded-md px-4 py-2 text-white focus:outline-none focus:border-blue-500">
        
        <select [(ngModel)]="roleFilter" class="bg-neutral-950 border border-neutral-800 rounded-md px-4 py-2 text-white outline-none">
          <option value="">All Roles</option>
          <option value="FOUNDER">Founder</option>
          <option value="INVESTOR">Investor</option>
          <option value="COFOUNDER">CoFounder</option>
        </select>

        <select [(ngModel)]="statusFilter" class="bg-neutral-950 border border-neutral-800 rounded-md px-4 py-2 text-white outline-none">
          <option value="">All Statuses</option>
          <option value="ACTIVE">Active</option>
          <option value="SUSPENDED">Suspended</option>
          <option value="BANNED">Banned</option>
        </select>

        <button (click)="loadUsers()" class="px-6 py-2 bg-blue-600 hover:bg-blue-500 text-white rounded-md transition-colors ml-auto flex items-center gap-2">
          <span class="material-icons text-sm">search</span> Search
        </button>
      </div>

      <!-- Table Section -->
      <div class="bg-neutral-950 border border-neutral-800 rounded-xl overflow-hidden shadow-xl shadow-black/40">
        <div class="overflow-x-auto">
          <table class="w-full text-left border-collapse">
            <thead>
              <tr class="bg-neutral-900 border-b border-neutral-800 text-neutral-400 text-sm">
                <th class="px-6 py-4 font-semibold">User</th>
                <th class="px-6 py-4 font-semibold">Role</th>
                <th class="px-6 py-4 font-semibold">Status</th>
                <th class="px-6 py-4 font-semibold whitespace-nowrap">Updated At</th>
                <th class="px-6 py-4 font-semibold text-right">Actions</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-neutral-800/60">
              <tr *ngFor="let user of users" class="hover:bg-neutral-900/50 transition-colors">
                <td class="px-6 py-4">
                  <div class="font-medium text-white">{{ user.name }}</div>
                  <div class="text-sm text-neutral-500">{{ user.email }}</div>
                </td>
                <td class="px-6 py-4">
                  <span class="px-2.5 py-1 rounded-md text-xs font-medium tracking-wide bg-neutral-800 text-neutral-300">
                    {{ user.role }}
                  </span>
                </td>
                <td class="px-6 py-4">
                  <span class="px-2.5 py-1 rounded-md text-xs font-medium tracking-wide"
                        [ngClass]="{
                          'bg-green-500/10 text-green-400 border border-green-500/20': user.status === 'ACTIVE' || !user.status,
                          'bg-orange-500/10 text-orange-400 border border-orange-500/20': user.status === 'SUSPENDED',
                          'bg-red-500/10 text-red-500 border border-red-500/20': user.status === 'BANNED'
                        }">
                    {{ user.status || 'ACTIVE' }}
                  </span>
                </td>
                <td class="px-6 py-4 text-sm text-neutral-400">
                  {{ user.updatedAt | date:'mediumDate' }}
                </td>
                <td class="px-6 py-4 text-right">
                  <div class="flex justify-end gap-2">
                    <button *ngIf="user.status !== 'SUSPENDED'" (click)="updateStatus(user.userId, 'SUSPENDED')" title="Suspend User"
                           class="p-2 text-orange-400 hover:bg-orange-400/10 rounded-md transition-colors">
                      <span class="material-icons text-sm">pause_circle</span>
                    </button>
                    <button *ngIf="user.status === 'SUSPENDED' || user.status === 'BANNED'" (click)="updateStatus(user.userId, 'ACTIVE')" title="Activate User"
                           class="p-2 text-green-400 hover:bg-green-400/10 rounded-md transition-colors">
                      <span class="material-icons text-sm">play_circle</span>
                    </button>
                    <button *ngIf="user.status !== 'BANNED'" (click)="updateStatus(user.userId, 'BANNED')" title="Ban User"
                           class="p-2 text-red-400 hover:bg-red-400/10 rounded-md transition-colors">
                      <span class="material-icons text-sm">block</span>
                    </button>
                  </div>
                </td>
              </tr>
              <tr *ngIf="users.length === 0">
                <td colspan="5" class="px-6 py-12 text-center text-neutral-500">
                  <span class="material-icons text-4xl block mb-2 opacity-50">person_off</span>
                  No users found matching the search criteria.
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        
        <!-- Pagination Component -->
        <div class="bg-neutral-900 border-t border-neutral-800 p-4 flex justify-between items-center text-sm text-neutral-400" *ngIf="totalPages > 1">
          <div>Page {{ page + 1 }} of {{ totalPages }}</div>
          <div class="flex gap-2">
            <button [disabled]="page === 0" (click)="changePage(page - 1)" class="px-3 py-1 bg-neutral-800 rounded hover:bg-neutral-700 disabled:opacity-50">Previous</button>
            <button [disabled]="page >= totalPages - 1" (click)="changePage(page + 1)" class="px-3 py-1 bg-neutral-800 rounded hover:bg-neutral-700 disabled:opacity-50">Next</button>
          </div>
        </div>
      </div>
    </div>
  `
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

  ngOnInit() {
    this.loadUsers();
  }

  loadUsers() {
    this.adminService.searchUsers(this.page, this.size, {
      name: this.searchName,
      email: this.searchEmail,
      role: this.roleFilter,
      status: this.statusFilter
    }).subscribe({
      next: (res) => {
        this.users = res.content || [];
        this.totalPages = res.totalPages || 0;
      },
      error: (err) => console.error(err)
    });
  }

  changePage(newPage: number) {
    this.page = newPage;
    this.loadUsers();
  }

  updateStatus(id: number, status: 'ACTIVE'|'SUSPENDED'|'BANNED') {
    if(confirm(`Are you sure you want to change user status to ${status}?`)) {
      this.adminService.updateUserStatus(id, { status }).subscribe({
        next: () => this.loadUsers(),
        error: (err) => alert('Failed to update user status')
      });
    }
  }
}
