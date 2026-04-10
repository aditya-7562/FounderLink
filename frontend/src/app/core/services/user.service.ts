import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { ApiEnvelope, PaginatedData, PaginationQuery, UserResponse, UserUpdateRequest } from '../../models';
import { normalizeCollection, normalizePlain, normalizeError } from './api-normalizer';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly api = environment.apiUrl;

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
  getAllUsers(query: PaginationQuery = {}): Observable<ApiEnvelope<PaginatedData<UserResponse>>> {
    return this.http.get<unknown>(`${this.api}/users`, { params: this.withPagination(query, 'id,asc') }).pipe(
      map(normalizeCollection<UserResponse>),
      catchError(err => throwError(() => normalizeError(err)))
    );
  }

  getUsersByRole(role: string, query: PaginationQuery = {}): Observable<ApiEnvelope<PaginatedData<UserResponse>>> {
    return this.http.get<unknown>(`${this.api}/users/role/${role}`, { params: this.withPagination(query, 'id,asc') }).pipe(
      map(normalizeCollection<UserResponse>),
      catchError(err => throwError(() => normalizeError(err)))
    );
  }

  /** Get aggregated public stats for landing page */
  getPublicStats(): Observable<{founders: number, investors: number, cofounders: number}> {
    return this.http.get<{founders: number, investors: number, cofounders: number}>(`${this.api}/users/public/stats`);
  }

  private withPagination(query: PaginationQuery, defaultSort: string): HttpParams {
    return new HttpParams()
      .set('page', query.page ?? 0)
      .set('size', query.size ?? 10)
      .set('sort', query.sort ?? defaultSort);
  }
}
