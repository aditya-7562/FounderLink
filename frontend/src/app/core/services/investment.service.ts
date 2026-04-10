import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError, timer, switchMap, take, catchError, of } from 'rxjs';
import { map, retry } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { ApiEnvelope, ApiResponse, InvestmentRequest, InvestmentResponse, InvestmentStatusUpdate, PaginatedData, PaginationQuery } from '../../models';
import { normalizeCollection, normalizeWrapped, normalizeError } from './api-normalizer';

@Injectable({ providedIn: 'root' })
export class InvestmentService {
  private readonly api = environment.apiUrl;

  constructor(private http: HttpClient) {}

  /** Investor: create a new investment */
  create(req: InvestmentRequest): Observable<ApiEnvelope<InvestmentResponse>> {
    return this.http.post<ApiResponse<InvestmentResponse>>(`${this.api}/investments`, req).pipe(
      map(normalizeWrapped),
      catchError(err => throwError(() => normalizeError(err)))
    );
  }

  /** Investor / Admin: get own investment portfolio */
  getMyPortfolio(query: PaginationQuery = {}): Observable<ApiEnvelope<PaginatedData<InvestmentResponse>>> {
    return this.http.get<unknown>(`${this.api}/investments/investor`, { params: this.withPagination(query) }).pipe(
      map(normalizeCollection<InvestmentResponse>),
      catchError(err => throwError(() => normalizeError(err)))
    );
  }

  /** Founder / Admin: get investments for a startup */
  getStartupInvestments(startupId: number, query: PaginationQuery = {}): Observable<ApiEnvelope<PaginatedData<InvestmentResponse>>> {
    return this.http.get<unknown>(`${this.api}/investments/startup/${startupId}`, { params: this.withPagination(query) }).pipe(
      map(normalizeCollection<InvestmentResponse>),
      catchError(err => throwError(() => normalizeError(err)))
    );
  }

  /** Get a single investment */
  getById(id: number): Observable<ApiEnvelope<InvestmentResponse>> {
    return this.http.get<ApiResponse<InvestmentResponse>>(`${this.api}/investments/${id}`).pipe(
      map(normalizeWrapped),
      catchError(err => throwError(() => normalizeError(err)))
    );
  }

  /** Founder / Admin: update investment status */
  updateStatus(id: number, update: InvestmentStatusUpdate): Observable<ApiEnvelope<InvestmentResponse>> {
    return this.http.put<ApiResponse<InvestmentResponse>>(`${this.api}/investments/${id}/status`, update).pipe(
      map(normalizeWrapped),
      catchError(err => throwError(() => normalizeError(err)))
    );
  }

  private withPagination(query: PaginationQuery): HttpParams {
    return new HttpParams()
      .set('page', query.page ?? 0)
      .set('size', query.size ?? 10)
      .set('sort', query.sort ?? 'createdAt,desc');
  }
}
