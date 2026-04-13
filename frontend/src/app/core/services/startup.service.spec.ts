import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { StartupService } from './startup.service';
import { environment } from '../../../environments/environment';
import { MOCK_STARTUP, MOCK_STARTUP_LIST } from '../../../testing/mocks/startup.mocks';

describe('StartupService', () => {
  let service: StartupService;
  let httpMock: HttpTestingController;
  const api = environment.apiUrl;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [StartupService]
    });
    service = TestBed.inject(StartupService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should construct pagination parameters correctly', () => {
    service.getAll({ page: 2, size: 20, sort: 'name,asc' }).subscribe();

    const req = httpMock.expectOne(request => 
      request.url === `${api}/startup` &&
      request.params.get('page') === '2' &&
      request.params.get('size') === '20' &&
      request.params.get('sort') === 'name,asc'
    );
    expect(req.request.method).toBe('GET');
  });

  it('should use default pagination if none provided', () => {
    service.getAll().subscribe(res => {
      expect(res.success).toBeTrue();
      expect(res.data!.page).toBe(0);
      expect(res.data!.size).toBe(9);
    });

    const req = httpMock.expectOne(request => 
      request.url === `${api}/startup` &&
      request.params.get('page') === '0' &&
      request.params.get('size') === '9' &&
      request.params.get('sort') === 'createdAt,desc'
    );
    req.flush({ 
      success: true, 
      data: { content: [], page: 0, size: 9, totalElements: 0, totalPages: 0, last: true } 
    });
  });

  it('should cache public stats to prevent redundant calls', () => {
    const mockStats = { startups: 10, totalFunding: 50000 };
    
    // First call
    service.getPublicStats().subscribe();
    httpMock.expectOne(`${api}/startup/public/stats`).flush(mockStats);

    // Second call - should NOT trigger a new HTTP request
    service.getPublicStats().subscribe(stats => {
      expect(stats).toEqual(mockStats);
    });
    httpMock.expectNone(`${api}/startup/public/stats`);
  });

  it('should append search filters correctly', () => {
    const filters = { industry: 'Fintech', minFunding: 1000, page: 1 };
    service.search(filters).subscribe();

    const req = httpMock.expectOne(request => 
      request.url === `${api}/startup/search` &&
      request.params.get('industry') === 'Fintech' &&
      request.params.get('minFunding') === '1000' &&
      request.params.get('page') === '1'
    );
    expect(req.request.method).toBe('GET');
  });

  it('should normalize one item response on getDetails', () => {
    service.getDetails(1).subscribe(res => {
      expect(res.success).toBeTrue();
      expect(res.data).toEqual(MOCK_STARTUP);
    });

    const req = httpMock.expectOne(`${api}/startup/details/1`);
    req.flush({ data: MOCK_STARTUP });
  });
});
