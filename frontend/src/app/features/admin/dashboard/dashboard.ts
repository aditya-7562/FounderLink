import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminService } from '../services/admin';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="animate-in fade-in slide-in-from-bottom-4 duration-500">
      <div class="mb-8 border-b border-neutral-800 pb-4">
        <h2 class="text-3xl font-bold tracking-tight text-white">Platform Overview</h2>
        <p class="text-neutral-400 mt-2">Real-time system statistics and health overview.</p>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        
        <!-- Total Users Card -->
        <div class="bg-neutral-950 border border-neutral-800 rounded-xl p-6 hover:border-blue-500/50 transition-colors shadow-lg shadow-black/20 relative overflow-hidden group">
          <div class="absolute inset-0 bg-gradient-to-br from-blue-500/10 to-transparent opacity-0 group-hover:opacity-100 transition-opacity"></div>
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-sm font-medium text-neutral-400 uppercase tracking-wider">Total Users</h3>
            <span class="material-icons text-blue-400 bg-blue-400/10 p-2 rounded-lg">people</span>
          </div>
          <div class="flex items-end gap-3">
            <span class="text-4xl font-bold text-white">{{ stats.users }}</span>
          </div>
        </div>

        <!-- Startups Card -->
        <div class="bg-neutral-950 border border-neutral-800 rounded-xl p-6 hover:border-purple-500/50 transition-colors shadow-lg shadow-black/20 relative overflow-hidden group">
          <div class="absolute inset-0 bg-gradient-to-br from-purple-500/10 to-transparent opacity-0 group-hover:opacity-100 transition-opacity"></div>
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-sm font-medium text-neutral-400 uppercase tracking-wider">Startups</h3>
            <span class="material-icons text-purple-400 bg-purple-400/10 p-2 rounded-lg">rocket_launch</span>
          </div>
          <div class="flex items-end gap-3">
            <span class="text-4xl font-bold text-white">{{ stats.startups }}</span>
          </div>
        </div>

        <!-- Banned Users Card -->
        <div class="bg-neutral-950 border border-neutral-800 rounded-xl p-6 hover:border-red-500/50 transition-colors shadow-lg shadow-black/20 relative overflow-hidden group">
          <div class="absolute inset-0 bg-gradient-to-br from-red-500/10 to-transparent opacity-0 group-hover:opacity-100 transition-opacity"></div>
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-sm font-medium text-neutral-400 uppercase tracking-wider">Banned Users</h3>
            <span class="material-icons text-red-400 bg-red-400/10 p-2 rounded-lg">gavel</span>
          </div>
          <div class="flex items-end gap-3">
            <span class="text-4xl font-bold text-white">{{ stats.banned }}</span>
          </div>
        </div>

        <!-- System Health Card -->
        <div class="bg-neutral-950 border border-neutral-800 rounded-xl p-6 hover:border-green-500/50 transition-colors shadow-lg shadow-black/20 relative overflow-hidden group">
          <div class="absolute inset-0 bg-gradient-to-br from-green-500/10 to-transparent opacity-0 group-hover:opacity-100 transition-opacity"></div>
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-sm font-medium text-neutral-400 uppercase tracking-wider">System Health</h3>
            <span class="material-icons text-green-400 bg-green-400/10 p-2 rounded-lg">health_and_safety</span>
          </div>
          <div class="flex items-end gap-3">
            <span class="text-3xl font-bold text-white tracking-widest text-green-400">OK</span>
          </div>
        </div>

      </div>
    </div>
  `
})
export class DashboardComponent implements OnInit {
  private adminService = inject(AdminService);
  
  stats = {
    users: 0,
    startups: 0,
    banned: 0
  };

  ngOnInit() {
    this.adminService.searchUsers(0, 1).subscribe(res => {
      this.stats.users = res.totalElements || 0;
    });

    this.adminService.searchUsers(0, 1, { status: 'BANNED' }).subscribe(res => {
      this.stats.banned = res.totalElements || 0;
    });

    this.adminService.getStartups(0, 1).subscribe(res => {
      // Accommodate structure whether standard ApiResponse<Page> or Page directly
      this.stats.startups = res.data?.totalElements || res.totalElements || 0;
    });
  }
}
