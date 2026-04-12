import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { map, catchError, shareReplay } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { ApiEnvelope, PaginatedData, PaginationQuery, UserResponse, UserUpdateRequest } from '../../models';
import { normalizeCollection, normalizePlain, normalizeError } from './api-normalizer';
import { AuthService } from './auth.service';

export interface UserSearchQuery extends PaginationQuery {
  keyword?: string;
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly api = environment.apiUrl;
  private publicStatsCache$?: Observable<{founders: number, investors: number, cofounders: number}>;

  constructor(private http: HttpClient, private auth: AuthService) {}

  /** Get user by id */
  getUser(id: number): Observable<ApiEnvelope<UserResponse>> {
    return this.http.get<UserResponse>(`${this.api}/users/${id}`).pipe(
      map(normalizePlain),
      catchError(err => throwError(() => normalizeError(err)))
    );
  }

  /** Update current user's profile — always uses session user id */
  updateMyProfile(payload: UserUpdateRequest): Observable<ApiEnvelope<UserResponse>> {
    const userId = this.auth.userId()!;
    return this.http.put<UserResponse>(`${this.api}/users/${userId}`, payload).pipe(
      map(normalizePlain),
      catchError(err => throwError(() => normalizeError(err)))
    );
  }

  /** Get all users (plain array response) */
  getAllUsers(query: UserSearchQuery = {}): Observable<ApiEnvelope<PaginatedData<UserResponse>>> {
    let params = this.withPagination(query, 'id,asc');
    if (query.keyword?.trim()) {
      params = params.set('keyword', query.keyword.trim());
    }
    return this.http.get<unknown>(`${this.api}/users`, { params }).pipe(
      map(normalizeCollection<UserResponse>),
      catchError(err => throwError(() => normalizeError(err)))
    );
  }

  getUsersByRole(role: string, query: UserSearchQuery = {}): Observable<ApiEnvelope<PaginatedData<UserResponse>>> {
    let params = this.withPagination(query, 'id,asc');
    if (query.keyword?.trim()) {
      params = params.set('keyword', query.keyword.trim());
    }
    return this.http.get<unknown>(`${this.api}/users/role/${role}`, { params }).pipe(
      map(normalizeCollection<UserResponse>),
      catchError(err => throwError(() => normalizeError(err)))
    );
  }

  /** Get aggregated public stats for landing page */
  getPublicStats(): Observable<{founders: number, investors: number, cofounders: number}> {
    if (!this.publicStatsCache$) {
      this.publicStatsCache$ = this.http.get<{founders: number, investors: number, cofounders: number}>(`${this.api}/users/public/stats`).pipe(
        shareReplay(1)
      );
    }
    return this.publicStatsCache$;
  }

  private withPagination(query: PaginationQuery, defaultSort: string): HttpParams {
    return new HttpParams()
      .set('page', query.page ?? 0)
      .set('size', query.size ?? 10)
      .set('sort', query.sort ?? defaultSort);
  }
}
