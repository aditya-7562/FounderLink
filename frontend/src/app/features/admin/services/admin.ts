import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { Observable, of, tap } from 'rxjs';

export interface UserStatusRequest {
  status: 'ACTIVE' | 'SUSPENDED' | 'BANNED';
  reason?: string;
}

export interface ModerationRequest {
  status: 'PENDING_REVIEW' | 'APPROVED' | 'REJECTED' | 'FLAGGED';
  reason?: string;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  private http = inject(HttpClient);

  readonly dashboardStats = signal<{users: number, startups: number, banned: number, health: any[]} | null>(null);
  readonly usersPage = signal<any>(null);
  readonly startupsPage = signal<any>(null);

  private usersCacheKey = '';
  private startupsCacheKey = '';

  searchUsers(page = 0, size = 10, search?: { name?: string; email?: string; role?: string; status?: string }): Observable<any> {
    let params: any = { page, size };
    if (search?.name) params.name = search.name;
    if (search?.email) params.email = search.email;
    if (search?.role) params.searchRole = search.role;
    if (search?.status) params.status = search.status;

    const key = JSON.stringify(params);
    if (key === this.usersCacheKey && this.usersPage()) {
      return of(this.usersPage());
    }

    return this.http.get<any>(`${environment.apiUrl}/users/admin/search`, { params }).pipe(
      tap(res => {
        this.usersPage.set(res);
        this.usersCacheKey = key;
      })
    );
  }

  getMicroservicesHealth(): Observable<any[]> {
    return this.http.get<any[]>(`${environment.apiUrl}/users/admin/health/microservices`);
  }

  updateUserStatus(id: number, request: UserStatusRequest) {
    return this.http.put<any>(`${environment.apiUrl}/users/admin/${id}/status`, request).pipe(
      tap(() => {
        this.usersPage.set(null);
        this.usersCacheKey = '';
        this.dashboardStats.set(null);
      })
    );
  }

  getStartups(page = 0, size = 9): Observable<any> {
    const key = JSON.stringify({ page, size });
    if (key === this.startupsCacheKey && this.startupsPage()) {
      return of(this.startupsPage());
    }

    return this.http.get<any>(`${environment.apiUrl}/startup`, { params: { page, size } }).pipe(
      tap(res => {
        this.startupsPage.set(res);
        this.startupsCacheKey = key;
      })
    );
  }

  updateStartupModeration(id: number, request: ModerationRequest) {
    return this.http.put<any>(`${environment.apiUrl}/startup/admin/${id}/moderation`, request).pipe(
      tap(() => {
        this.startupsPage.set(null);
        this.startupsCacheKey = '';
        this.dashboardStats.set(null);
      })
    );
  }

  deleteStartup(id: number) {
    return this.http.delete<any>(`${environment.apiUrl}/startup/admin/${id}`).pipe(
      tap(() => {
        this.startupsPage.set(null);
        this.startupsCacheKey = '';
        this.dashboardStats.set(null);
      })
    );
  }
}
