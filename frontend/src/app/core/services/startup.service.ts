import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiEnvelope, ApiResponse, PaginationQuery, StartupRequest, StartupResponse, PaginatedData } from '../../models';
import { normalizeCollection, normalizeWrapped, normalizeError } from './api-normalizer';

@Injectable({ providedIn: 'root' })
export class StartupService {
  private readonly api = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getAll(query: PaginationQuery = {}): Observable<ApiEnvelope<PaginatedData<StartupResponse>>> {
    return this.http.get<unknown>(`${this.api}/startup`, { params: this.withPagination(query, 'createdAt,desc') }).pipe(
      map(normalizeCollection<StartupResponse>),
      catchError(err => throwError(() => normalizeError(err)))
    );
  }

  search(filters: { industry?: string; stage?: string; minFunding?: number; maxFunding?: number } & PaginationQuery): Observable<ApiEnvelope<PaginatedData<StartupResponse>>> {
    let params = this.withPagination(filters, 'createdAt,desc');
    if (filters.industry)  params = params.set('industry', filters.industry);
    if (filters.stage)     params = params.set('stage', filters.stage);
    if (filters.minFunding != null) params = params.set('minFunding', filters.minFunding);
    if (filters.maxFunding != null) params = params.set('maxFunding', filters.maxFunding);
    return this.http.get<unknown>(`${this.api}/startup/search`, { params }).pipe(
      map(normalizeCollection<StartupResponse>),
      catchError(err => throwError(() => normalizeError(err)))
    );
  }

  getDetails(id: number): Observable<ApiEnvelope<StartupResponse>> {
    return this.http.get<ApiResponse<StartupResponse>>(`${this.api}/startup/details/${id}`).pipe(
      map(body => normalizeWrapped<StartupResponse>(body)),
      catchError(err => throwError(() => normalizeError(err)))
    );
  }

  getMyStartups(query: PaginationQuery = {}): Observable<ApiEnvelope<PaginatedData<StartupResponse>>> {
    return this.http.get<unknown>(`${this.api}/startup/founder`, { params: this.withPagination(query, 'createdAt,desc') }).pipe(
      map(normalizeCollection<StartupResponse>),
      catchError(err => throwError(() => normalizeError(err)))
    );
  }

  create(req: StartupRequest): Observable<ApiEnvelope<StartupResponse>> {
    return this.http.post<ApiResponse<StartupResponse>>(`${this.api}/startup`, req).pipe(
      map(body => normalizeWrapped<StartupResponse>(body)),
      catchError(err => throwError(() => normalizeError(err)))
    );
  }

  update(id: number, req: StartupRequest): Observable<ApiEnvelope<StartupResponse>> {
    return this.http.put<ApiResponse<StartupResponse>>(`${this.api}/startup/${id}`, req).pipe(
      map(body => normalizeWrapped<StartupResponse>(body)),
      catchError(err => throwError(() => normalizeError(err)))
    );
  }

  delete(id: number): Observable<ApiEnvelope<null>> {
    return this.http.delete<ApiResponse<null>>(`${this.api}/startup/${id}`).pipe(
      map(body => normalizeWrapped<null>(body)),
      catchError(err => throwError(() => normalizeError(err)))
    );
  }

  private withPagination(query: PaginationQuery, defaultSort: string): HttpParams {
    const page = query.page ?? 0;
    const size = query.size ?? 10;
    const sort = query.sort ?? defaultSort;

    return new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sort', sort);
  }
}
