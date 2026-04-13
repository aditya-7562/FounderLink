import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { errorInterceptor } from './error.interceptor';
import { NotificationService } from '../services/notification.service';

describe('errorInterceptor', () => {
  let httpMock: HttpTestingController;
  let httpClient: HttpClient;
  let notificationServiceSpy: jasmine.SpyObj<NotificationService>;

  beforeEach(() => {
    notificationServiceSpy = jasmine.createSpyObj('NotificationService', ['showUIError']);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        { provide: NotificationService, useValue: notificationServiceSpy }
      ]
    });

    httpClient = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should notify UI for 403 Forbidden errors', () => {
    httpClient.get('/api/admin').subscribe({ error: () => {} });

    httpMock.expectOne('/api/admin').flush('Forbidden', { status: 403, statusText: 'Forbidden' });

    expect(notificationServiceSpy.showUIError).toHaveBeenCalledWith(
      'Forbidden',
      jasmine.stringMatching(/permission/)
    );
  });

  it('should notify UI for 404 Not Found errors', () => {
    httpClient.get('/api/missing').subscribe({ error: () => {} });

    httpMock.expectOne('/api/missing').flush('Not Found', { status: 404, statusText: 'Not Found' });

    expect(notificationServiceSpy.showUIError).toHaveBeenCalledWith(
      'Not Found',
      jasmine.stringMatching(/could not be found/)
    );
  });

  it('should NOT notify UI for 401 Unauthorized errors (handled by auth interceptor)', () => {
    httpClient.get('/api/secure').subscribe({ error: () => {} });

    httpMock.expectOne('/api/secure').flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

    expect(notificationServiceSpy.showUIError).not.toHaveBeenCalled();
  });

  it('should notified UI for generic server errors with custom message', () => {
    const errorBody = { message: 'Custom database failure' };
    httpClient.get('/api/buggy').subscribe({ error: () => {} });

    httpMock.expectOne('/api/buggy').flush(errorBody, { status: 500, statusText: 'Server Error' });

    expect(notificationServiceSpy.showUIError).toHaveBeenCalledWith(
      'Server Error (500)',
      'Custom database failure'
    );
  });
});
