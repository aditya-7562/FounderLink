import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TeamService } from './team.service';
import { environment } from '../../../environments/environment';

describe('TeamService', () => {
  let service: TeamService;
  let httpMock: HttpTestingController;
  const api = environment.apiUrl;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [TeamService]
    });
    service = TestBed.inject(TeamService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should construct invitations pagination correctly', () => {
    service.getMyInvitations({ page: 0, size: 5, sort: 'createdAt,asc' }).subscribe();

    const req = httpMock.expectOne(request => 
      request.url === `${api}/teams/invitations/user` &&
      request.params.get('page') === '0' &&
      request.params.get('size') === '5' &&
      request.params.get('sort') === 'createdAt,asc'
    );
    expect(req.request.method).toBe('GET');
  });

  it('should handle sendInvitation correctly', () => {
    const reqBody = { startupId: 1, invitedUserId: 22, role: 'CTO' as const };
    service.sendInvitation(reqBody).subscribe(res => {
      expect(res.success).toBeTrue();
    });

    const req = httpMock.expectOne(`${api}/teams/invite`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(reqBody);
    req.flush({ data: { id: 101, ...reqBody, status: 'PENDING' } });
  });

  it('should handle cancelInvitation correctly', () => {
    service.cancelInvitation(101).subscribe(res => {
      expect(res.success).toBeTrue();
    });

    const req = httpMock.expectOne(`${api}/teams/invitations/101/cancel`);
    expect(req.request.method).toBe('PUT');
    req.flush({ data: { id: 101, status: 'CANCELLED' } });
  });

  it('should compute startup-specific invitation lists', () => {
    service.getStartupInvitations(1).subscribe();
    const req = httpMock.expectOne(request => request.url === `${api}/teams/invitations/startup/1`);
    expect(req.request.method).toBe('GET');
  });

  it('should compute active team members for a startup', () => {
    service.getTeamMembers(1).subscribe();
    const req = httpMock.expectOne(request => 
      request.url === `${api}/teams/startup/1` &&
      request.params.get('sort') === 'joinedAt,desc'
    );
    expect(req.request.method).toBe('GET');
  });
});
