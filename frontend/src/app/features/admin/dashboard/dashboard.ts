import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminService } from '../services/admin';
import { forkJoin, of, catchError } from 'rxjs';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class DashboardComponent implements OnInit {
  private adminService = inject(AdminService);
  
  loading = signal(true);

  stats = {
    users: 0,
    startups: 0,
    banned: 0
  };
  healthData = signal<any[]>([]);

  ngOnInit() {
    const cached = this.adminService.dashboardStats();
    if (cached) {
      this.stats = { users: cached.users, startups: cached.startups, banned: cached.banned };
      this.healthData.set(cached.health);
      this.loading.set(false);
      return;
    }

    forkJoin({
      users: this.adminService.searchUsers(0, 1).pipe(catchError(() => of({ totalElements: 0 }))),
      banned: this.adminService.searchUsers(0, 1, { status: 'BANNED' }).pipe(catchError(() => of({ totalElements: 0 }))),
      startups: this.adminService.getStartups(0, 1).pipe(catchError(() => of({ totalElements: 0 }))),
      health: this.adminService.getMicroservicesHealth().pipe(catchError(() => of([])))
    }).subscribe({
      next: (res: any) => {
        const newStats = {
          users: res.users.totalElements || 0,
          banned: res.banned.totalElements || 0,
          startups: res.startups.data?.totalElements || res.startups.totalElements || 0,
          health: res.health || []
        };
        this.stats = { users: newStats.users, startups: newStats.startups, banned: newStats.banned };
        this.healthData.set(newStats.health);
        
        this.adminService.dashboardStats.set(newStats);
        this.loading.set(false);
      },
      error: (err) => {
        console.error(err);
        this.loading.set(false);
      }
    });
  }
}
