import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { errorInterceptor } from './interceptors/error.interceptor';
import { authInterceptor } from './interceptors/auth.interceptor';
import { NotificationService } from './services/notification.service';
import { AuthService } from './services/auth.service';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { normalizeCollection, normalizeError } from './services/api-normalizer';

describe('Phase 3: Defensive Failure Validation', () => {
  let httpMock: HttpTestingController;
  let httpClient: HttpClient;
  let notificationServiceSpy: jasmine.SpyObj<NotificationService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(() => {
    notificationServiceSpy = jasmine.createSpyObj('NotificationService', ['showUIError']);
    authServiceSpy = jasmine.createSpyObj('AuthService', ['token', 'refresh', 'clearSession']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor, errorInterceptor])),
        provideHttpClientTesting(),
        { provide: NotificationService, useValue: notificationServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    });

    httpClient = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('Server Crash Scenarios (500, 503, 504)', () => {
    it('should propagate 500 Internal Server Error cleanly with UI notification', () => {
      httpClient.get('/api/test').subscribe({
        error: (err) => expect(err.error).toBeDefined()
      });

      const req = httpMock.expectOne('/api/test');
      req.flush({ message: 'Database explosion' }, { status: 500, statusText: 'Internal Server Error' });

      expect(notificationServiceSpy.showUIError).toHaveBeenCalledWith(
        'Server Error (500)',
        'Database explosion'
      );
    });

    it('should handle 503 Service Unavailable (Gateway timeout)', () => {
      httpClient.get('/api/test').subscribe({ error: () => {} });
      const req = httpMock.expectOne('/api/test');
      req.flush(null, { status: 503, statusText: 'Service Unavailable' });

      expect(notificationServiceSpy.showUIError).toHaveBeenCalledWith(
        'Server Error (503)',
        jasmine.stringMatching(/503/)
      );
    });
  });

  describe('Garbage & Malformed Response Handling', () => {
    it('should NOT crash when API returns completely unmapped JSON', () => {
      httpClient.get('/api/test').subscribe(res => {
        const normalized = normalizeCollection(res);
        expect(normalized.success).toBeTrue();
        expect(normalized.data!.content).toEqual([]);
        expect(normalized.data!.totalElements).toBe(0);
      });

      const req = httpMock.expectOne('/api/test');
      req.flush({ some_weird_key: 'random value', nested: { nothing: true } });
    });

    it('should handle null body gracefully in normalization', () => {
      const res = normalizeCollection(null);
      expect(res.data!.content).toEqual([]);
      expect(res.data!.totalElements).toBe(0);
    });

    it('should handle partial page objects missing content array', () => {
      const partialBody = { totalElements: 100, page: 0 };
      const normalized = normalizeCollection(partialBody);
      expect(normalized.data!.content).toEqual([]);
      expect(normalized.data!.totalElements).toBe(0);
    });
  });

  describe('Authentication & Authorization Hard-Failure', () => {
    it('should navigate to login on 401 if refresh is not applicable or fails', () => {
      authServiceSpy.refresh.and.returnValue(throwError(() => new Error('Hard fail')));
      
      httpClient.get('/api/secure').subscribe({ error: () => {} });
      
      const req = httpMock.expectOne('/api/secure');
      req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

      expect(routerSpy.navigate).toHaveBeenCalledWith(['/auth/login']);
    });

    it('should show Forbidden message on 403 status', () => {
      httpClient.get('/api/admin-only').subscribe({ error: () => {} });
      
      const req = httpMock.expectOne('/api/admin-only');
      req.flush('Forbidden', { status: 403, statusText: 'Forbidden' });

      expect(notificationServiceSpy.showUIError).toHaveBeenCalledWith(
        'Forbidden',
        jasmine.stringMatching(/permission/)
      );
    });
  });

  describe('Network & Connectivity Errors', () => {
    it('should handle "0 Unknown Error" (CORS or Network Drop)', () => {
      httpClient.get('/api/test').subscribe({ error: () => {} });
      
      const req = httpMock.expectOne('/api/test');
      req.error(new ProgressEvent('error')); // Simulates network drop

      expect(notificationServiceSpy.showUIError).toHaveBeenCalledWith(
        'Server Error (0)',
        jasmine.stringMatching(/: 0/)
      );
    });
  });
});
