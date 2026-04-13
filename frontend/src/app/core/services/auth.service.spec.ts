import { TestBed } from '@angular/core/testing';
import { HttpClient } from '@angular/common/http';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { AuthResponse } from '../../models';
import { environment } from '../../../environments/environment';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let routerSpy = jasmine.createSpyObj('Router', ['navigate']);

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        AuthService,
        { provide: Router, useValue: routerSpy }
      ]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    localStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('Initialization', () => {
    it('should initial state from localStorage', () => {
      localStorage.setItem('token', 'fake-token');
      localStorage.setItem('userId', '123');
      localStorage.setItem('role', 'FOUNDER');
      
      const httpClient = TestBed.inject(HttpClient);
      const newService = new AuthService(httpClient, routerSpy);
      
      expect(newService.token()).toBe('fake-token');
      expect(newService.userId()).toBe(123);
      expect(newService.isLoggedIn()).toBeTrue();
    });
  });

  it('should store session and update signals on login', () => {
    const mockResponse: AuthResponse = {
      token: 'new-token',
      userId: 456,
      role: 'ROLE_FOUNDER',
      email: 'test@founderlink.com'
    };

    service.login({ email: 'test@test.com', password: 'password' }).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse);

    expect(localStorage.getItem('token')).toBe('new-token');
    expect(service.token()).toBe('new-token');
    expect(service.userId()).toBe(456);
    expect(service.role()).toBe('ROLE_FOUNDER');
    expect(service.isLoggedIn()).toBeTrue();
  });

  it('should clear session and navigate to login on logout', () => {
    localStorage.setItem('token', 'exists');
    service.clearSession();

    expect(localStorage.getItem('token')).toBeNull();
    expect(service.token()).toBeNull();
    expect(service.isLoggedIn()).toBeFalse();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/auth/login']);
  });

  it('should compute correct home route based on role', () => {
    expect(service.getHomeRoute('ROLE_ADMIN')).toBe('/admin/dashboard');
    expect(service.getHomeRoute('FOUNDER')).toBe('/dashboard');
    expect(service.getHomeRoute(null as any)).toBe('/dashboard');
  });
});
