import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

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

  searchUsers(page = 0, size = 10, search?: { name?: string; email?: string; role?: string; status?: string }) {
    let params: any = { page, size };
    if (search?.name) params.name = search.name;
    if (search?.email) params.email = search.email;
    if (search?.role) params.role = search.role;
    if (search?.status) params.status = search.status;
    return this.http.get<any>(`${environment.apiUrl}/users/admin/search`, { params });
  }

  updateUserStatus(id: number, request: UserStatusRequest) {
    return this.http.put<any>(`${environment.apiUrl}/users/admin/${id}/status`, request);
  }

  getStartups(page = 0, size = 9) {
    return this.http.get<any>(`${environment.apiUrl}/startup`, { params: { page, size } });
  }

  updateStartupModeration(id: number, request: ModerationRequest) {
    return this.http.put<any>(`${environment.apiUrl}/startup/admin/${id}/moderation`, request);
  }

  deleteStartup(id: number) {
    return this.http.delete<any>(`${environment.apiUrl}/startup/admin/${id}`);
  }
}
