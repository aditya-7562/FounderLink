import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { ApiEnvelope, MessageResponse, PaginatedData, PaginationQuery, UserResponse, CursorPage } from '../../models';
import { normalizeCollection, normalizePlain, normalizeError } from './api-normalizer';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class MessagingService {
  private readonly api = environment.apiUrl;

  constructor(private http: HttpClient, private auth: AuthService) {}

  /**
   * Send a message.
   * senderId is ALWAYS derived from the authenticated session — never user-controlled.
   */
  sendMessage(receiverId: number, content: string): Observable<ApiEnvelope<MessageResponse>> {
    const senderId = this.auth.userId()!;
    return this.http.post<MessageResponse>((`${this.api}/messages`), { senderId, receiverId, content }).pipe(
      map(normalizePlain),
      catchError(err => throwError(() => normalizeError(err)))
    );
  }

  /** Get full conversation between current user and a partner (plain array response) */
  getConversation(partnerId: number, query: PaginationQuery = {}): Observable<ApiEnvelope<PaginatedData<MessageResponse>>> {
    const userId = this.auth.userId()!;
    return this.http.get<unknown>(`${this.api}/messages/conversation/${userId}/${partnerId}`, {
      params: this.withPagination(query, 'createdAt,desc')
    }).pipe(
      map(normalizeCollection<MessageResponse>),
      catchError(err => throwError(() => normalizeError(err)))
    );
  }

  /**
   * Get messages using cursor-based pagination.
   */
  getConversationCursor(partnerId: number, params: {
    before?: number; after?: number; size?: number;
  }): Observable<ApiEnvelope<CursorPage<MessageResponse>>> {
    const userId = this.auth.userId()!;
    let httpParams = new HttpParams();
    if (params.before !== undefined) httpParams = httpParams.set('before', params.before);
    if (params.after !== undefined) httpParams = httpParams.set('after', params.after);
    if (params.size !== undefined) httpParams = httpParams.set('size', params.size);

    return this.http.get<CursorPage<MessageResponse>>(`${this.api}/messages/conversation/${userId}/${partnerId}/cursor`, {
      params: httpParams
    }).pipe(
      map(normalizePlain), // Since the endpoint returns ResponseEntity containing a single CursorPageDTO object, normalizePlain works.
      catchError(err => throwError(() => normalizeError(err)))
    );
  }

  /**
   * Get distinct user IDs that have messaged with current user.
   * Returns plain number[] — not objects.
   */
  getPartnerIds(query: PaginationQuery = {}): Observable<ApiEnvelope<PaginatedData<number>>> {
    const userId = this.auth.userId()!;
    return this.http.get<unknown>(`${this.api}/messages/partners/${userId}`, {
      params: this.withPagination(query, 'createdAt,desc')
    }).pipe(
      map(normalizeCollection<number>),
      catchError(err => throwError(() => normalizeError(err)))
    );
  }

  /** Get message by id */
  getById(id: number): Observable<ApiEnvelope<MessageResponse>> {
    return this.http.get<MessageResponse>(`${this.api}/messages/${id}`).pipe(
      map(normalizePlain),
      catchError(err => throwError(() => normalizeError(err)))
    );
  }

  private withPagination(query: PaginationQuery, defaultSort: string): HttpParams {
    return new HttpParams()
      .set('page', query.page ?? 0)
      .set('size', query.size ?? 10)
      .set('sort', query.sort ?? defaultSort);
  }
}
