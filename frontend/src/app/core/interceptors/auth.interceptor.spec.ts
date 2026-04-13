import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { authInterceptor, resetAuthInterceptor } from './auth.interceptor';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthResponse } from '../../models';

describe('authInterceptor', () => {
  let httpMock: HttpTestingController;
  let httpClient: HttpClient;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(() => {
    resetAuthInterceptor();
    authServiceSpy = jasmine.createSpyObj('AuthService', ['token', 'refresh']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
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

  it('should add Authorization header if token exists and route is not public', () => {
    authServiceSpy.token.and.returnValue('valid-token');

    httpClient.get('/api/secure-data').subscribe();

    const req = httpMock.expectOne('/api/secure-data');
    expect(req.request.headers.has('Authorization')).toBeTrue();
    expect(req.request.headers.get('Authorization')).toBe('Bearer valid-token');
  });

  it('should NOT add Authorization header for public auth endpoints', () => {
    authServiceSpy.token.and.returnValue('valid-token');

    httpClient.post('/api/auth/login', {}).subscribe();

    const req = httpMock.expectOne('/api/auth/login');
    expect(req.request.headers.has('Authorization')).toBeFalse();
  });

  it('should attempt token refresh on 401 error', () => {
    authServiceSpy.token.and.returnValue('expired-token');
    const refreshResponse: AuthResponse = { token: 'new-token', userId: 1, role: 'USER', email: 'a@b.com' };
    authServiceSpy.refresh.and.returnValue(of(refreshResponse));

    httpClient.get('/api/orders').subscribe();

    // Initial request fails with 401
    const initialReq = httpMock.expectOne('/api/orders');
    initialReq.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

    // Expect refresh call
    expect(authServiceSpy.refresh).toHaveBeenCalled();

    // Expect retry of original request with NEW token
    const retryReq = httpMock.expectOne('/api/orders');
    expect(retryReq.request.headers.get('Authorization')).toBe('Bearer new-token');
  });

  it('should navigate to login if refresh fails', () => {
    authServiceSpy.token.and.returnValue('expired-token');
    authServiceSpy.refresh.and.returnValue(throwError(() => new Error('Refresh failed')));

    httpClient.get('/api/orders').subscribe({
      error: () => {}
    });

    const initialReq = httpMock.expectOne('/api/orders');
    initialReq.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

    expect(routerSpy.navigate).toHaveBeenCalledWith(['/auth/login']);
  });
});
