import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import {
  ApiEnvelope, ApiResponse,
  InvitationRequest, InvitationResponse,
  JoinTeamRequest, PaginatedData, PaginationQuery, TeamMemberResponse
} from '../../models';
import { normalizeCollection, normalizeWrapped, normalizeError } from './api-normalizer';

@Injectable({ providedIn: 'root' })
export class TeamService {
  private readonly api = environment.apiUrl;

  constructor(private http: HttpClient) {}

  /** Founder: send invitation */
  sendInvitation(req: InvitationRequest): Observable<ApiEnvelope<InvitationResponse>> {
    return this.http.post<ApiResponse<InvitationResponse>>(`${this.api}/teams/invite`, req).pipe(
      map(normalizeWrapped),
      catchError(err => throwError(() => normalizeError(err)))
    );
  }

  /** Founder: cancel pending invitation */
  cancelInvitation(id: number): Observable<ApiEnvelope<InvitationResponse>> {
    return this.http.put<ApiResponse<InvitationResponse>>(`${this.api}/teams/invitations/${id}/cancel`, {}).pipe(
      map(normalizeWrapped),
      catchError(err => throwError(() => normalizeError(err)))
    );
  }

  /** CoFounder: reject pending invitation */
  rejectInvitation(id: number): Observable<ApiEnvelope<InvitationResponse>> {
    return this.http.put<ApiResponse<InvitationResponse>>(`${this.api}/teams/invitations/${id}/reject`, {}).pipe(
      map(normalizeWrapped),
      catchError(err => throwError(() => normalizeError(err)))
    );
  }

  /** CoFounder: accept invitation and join team */
  joinTeam(req: JoinTeamRequest): Observable<ApiEnvelope<TeamMemberResponse>> {
    return this.http.post<ApiResponse<TeamMemberResponse>>(`${this.api}/teams/join`, req).pipe(
      map(normalizeWrapped),
      catchError(err => throwError(() => normalizeError(err)))
    );
  }

  /** CoFounder: get own pending invitations */
  getMyInvitations(query: PaginationQuery = {}): Observable<ApiEnvelope<PaginatedData<InvitationResponse>>> {
    return this.http.get<unknown>(`${this.api}/teams/invitations/user`, { params: this.withPagination(query, 'createdAt,desc') }).pipe(
      map(normalizeCollection<InvitationResponse>),
      catchError(err => throwError(() => normalizeError(err)))
    );
  }

  /** Founder: get invitations for a startup */
  getStartupInvitations(startupId: number, query: PaginationQuery = {}): Observable<ApiEnvelope<PaginatedData<InvitationResponse>>> {
    return this.http.get<unknown>(`${this.api}/teams/invitations/startup/${startupId}`, { params: this.withPagination(query, 'createdAt,desc') }).pipe(
      map(normalizeCollection<InvitationResponse>),
      catchError(err => throwError(() => normalizeError(err)))
    );
  }

  /** Get active team members for a startup */
  getTeamMembers(startupId: number, query: PaginationQuery = {}): Observable<ApiEnvelope<PaginatedData<TeamMemberResponse>>> {
    return this.http.get<unknown>(`${this.api}/teams/startup/${startupId}`, { params: this.withPagination(query, 'joinedAt,desc') }).pipe(
      map(normalizeCollection<TeamMemberResponse>),
      catchError(err => throwError(() => normalizeError(err)))
    );
  }

  /** Founder: remove a team member */
  removeMember(teamMemberId: number): Observable<ApiEnvelope<null>> {
    return this.http.delete<ApiResponse<null>>(`${this.api}/teams/${teamMemberId}`).pipe(
      map(normalizeWrapped),
      catchError(err => throwError(() => normalizeError(err)))
    );
  }

  /** CoFounder / Admin: get active roles for current user */
  getMyActiveRoles(query: PaginationQuery = {}): Observable<ApiEnvelope<PaginatedData<TeamMemberResponse>>> {
    return this.http.get<unknown>(`${this.api}/teams/member/active`, { params: this.withPagination(query, 'joinedAt,desc') }).pipe(
      map(normalizeCollection<TeamMemberResponse>),
      catchError(err => throwError(() => normalizeError(err)))
    );
  }

  /** CoFounder / Admin: get full member history for current user */
  getMemberHistory(query: PaginationQuery = {}): Observable<ApiEnvelope<PaginatedData<TeamMemberResponse>>> {
    return this.http.get<unknown>(`${this.api}/teams/member/history`, { params: this.withPagination(query, 'joinedAt,desc') }).pipe(
      map(normalizeCollection<TeamMemberResponse>),
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
