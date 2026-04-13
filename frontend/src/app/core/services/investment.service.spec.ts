import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { InvestmentService } from './investment.service';
import { environment } from '../../../environments/environment';

describe('InvestmentService', () => {
  let service: InvestmentService;
  let httpMock: HttpTestingController;
  const api = environment.apiUrl;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [InvestmentService]
    });
    service = TestBed.inject(InvestmentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should handle investment creation correctly', () => {
    const reqBody = { startupId: 1, amount: 25000 };
    service.create(reqBody as any).subscribe(res => {
      expect(res.success).toBeTrue();
    });

    const req = httpMock.expectOne(`${api}/investments`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(reqBody);
    req.flush({ data: { id: 501, ...reqBody, status: 'PENDING' } });
  });

  it('should construct portfolio query with default sort', () => {
    service.getMyPortfolio().subscribe();

    const req = httpMock.expectOne(request => 
      request.url === `${api}/investments/investor` &&
      request.params.get('sort') === 'createdAt,desc'
    );
    expect(req.request.method).toBe('GET');
  });

  it('should fetch startup-specific investments', () => {
    service.getStartupInvestments(1).subscribe();
    const req = httpMock.expectOne(request => request.url === `${api}/investments/startup/1`);
    expect(req.request.method).toBe('GET');
  });

  it('should handle status updates', () => {
    const update = { status: 'APPROVED' as const };
    service.updateStatus(501, update).subscribe(res => {
      expect(res.success).toBeTrue();
    });

    const req = httpMock.expectOne(`${api}/investments/501/status`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(update);
    req.flush({ data: { id: 501, status: 'APPROVED' } });
  });
});
