import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { ApiEnvelope, NotificationResponse, PaginatedData, PaginationQuery } from '../../models';
import { normalizeCollection, normalizePlain, normalizeError } from './api-normalizer';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly api = environment.apiUrl;

  constructor(private http: HttpClient, private auth: AuthService) {}

  /** Get all notifications for the logged-in user (plain array response) */
  getMyNotifications(query: PaginationQuery = {}): Observable<ApiEnvelope<PaginatedData<NotificationResponse>>> {
    const userId = this.auth.userId()!;
    return this.http.get<unknown>(`${this.api}/notifications/${userId}`, {
      params: this.withPagination(query)
    }).pipe(
      map(normalizeCollection<NotificationResponse>),
      catchError(err => throwError(() => normalizeError(err)))
    );
  }

  /** Get unread notifications for the logged-in user (plain array response) */
  getMyUnreadNotifications(query: PaginationQuery = {}): Observable<ApiEnvelope<PaginatedData<NotificationResponse>>> {
    const userId = this.auth.userId()!;
    return this.http.get<unknown>(`${this.api}/notifications/${userId}/unread`, {
      params: this.withPagination(query)
    }).pipe(
      map(normalizeCollection<NotificationResponse>),
      catchError(err => throwError(() => normalizeError(err)))
    );
  }

  /** Mark a notification as read */
  markAsRead(id: number): Observable<ApiEnvelope<NotificationResponse>> {
    return this.http.put<NotificationResponse>(`${this.api}/notifications/${id}/read`, {}).pipe(
      map(normalizePlain),
      catchError(err => throwError(() => normalizeError(err)))
    );
  }

  private withPagination(query: PaginationQuery): HttpParams {
    return new HttpParams()
      .set('page', query.page ?? 0)
      .set('size', query.size ?? 10)
      .set('sort', query.sort ?? 'createdAt,desc');
  }

  /** Global UI error display handler */
  showUIError(title: string, message: string): void {
    console.error(`[Global Error] ${title}: ${message}`);
  }
}
