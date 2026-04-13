import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { cacheInterceptor, clearCache } from './cache.interceptor';

describe('cacheInterceptor', () => {
  let httpMock: HttpTestingController;
  let httpClient: HttpClient;

  beforeEach(() => {
    clearCache();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([cacheInterceptor])),
        provideHttpClientTesting()
      ]
    });

    httpClient = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should cache requests for cacheable URLs', fakeAsync(() => {
    const cacheableUrl = '/users/public/stats';
    const mockResponse = { data: 'test' };

    // First request
    httpClient.get(cacheableUrl).subscribe();
    const req1 = httpMock.expectOne(cacheableUrl);
    req1.flush(mockResponse);

    // Second request (should be cached)
    let cachedResult: any;
    httpClient.get(cacheableUrl).subscribe(res => cachedResult = res);
    
    // No request should go to the backend this time
    httpMock.expectNone(cacheableUrl);
    expect(cachedResult).toEqual(mockResponse);
  }));

  it('should expire cache after TTL', fakeAsync(() => {
    const cacheableUrl = '/users/public/stats';
    httpClient.get(cacheableUrl).subscribe();
    httpMock.expectOne(cacheableUrl).flush({ data: 'old' });

    // Advance time beyond 30s TTL
    tick(31000);

    // Third request (cache should be expired)
    let freshResult: any;
    httpClient.get(cacheableUrl).subscribe(res => freshResult = res);
    
    const req2 = httpMock.expectOne(cacheableUrl); // Expect a new network request
    req2.flush({ data: 'new' });
    expect(freshResult.data).toBe('new');
  }));

  it('should NOT cache POST requests', () => {
    const cacheableUrl = '/users/public/stats';
    httpClient.post(cacheableUrl, {}).subscribe();
    const req1 = httpMock.expectOne(cacheableUrl);
    req1.flush({});

    httpClient.post(cacheableUrl, {}).subscribe();
    const req2 = httpMock.expectOne(cacheableUrl); // Still goes to network because it's POST
    expect(req2).toBeDefined();
    req2.flush({});
  });
});
