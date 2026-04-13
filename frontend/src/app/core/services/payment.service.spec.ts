import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { PaymentService } from './payment.service';
import { environment } from '../../../environments/environment';

describe('PaymentService', () => {
  let service: PaymentService;
  let httpMock: HttpTestingController;
  const api = environment.apiUrl;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [PaymentService]
    });
    service = TestBed.inject(PaymentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('pollPaymentAvailability', () => {
    it('should retry on 404 and eventually return success', fakeAsync(() => {
      const investmentId = 123;
      const mockPayment = { id: 1, amount: 1000 };
      
      let result: any;
      service.pollPaymentAvailability(investmentId).subscribe(res => result = res);

      // Attempt 1: 404
      const req1 = httpMock.expectOne(`${api}/payments/investment/${investmentId}`);
      req1.flush('Not Found', { status: 404, statusText: 'Not Found' });
      
      tick(2000); // Wait for POLL_INTERVAL_MS

      // Attempt 2: Success
      const req2 = httpMock.expectOne(`${api}/payments/investment/${investmentId}`);
      req2.flush({ data: mockPayment });

      expect(result.success).toBeTrue();
      expect(result.data).toEqual(mockPayment);
    }));

    it('should stop polling and throw error after 5 retries', fakeAsync(() => {
      const investmentId = 123;
      let errorResponse: any;

      service.pollPaymentAvailability(investmentId).subscribe({
        error: (err) => errorResponse = err
      });

      // 5 failed attempts
      for (let i = 0; i < 5; i++) {
        const req = httpMock.expectOne(`${api}/payments/investment/${investmentId}`);
        req.flush('Not Found', { status: 404, statusText: 'Not Found' });
        tick(2000);
      }

      expect(errorResponse.success).toBeFalse();
      expect(errorResponse.error).toBe('Not Found');
    }));

    it('should stop polling immediately on non-404 errors (e.g., 403)', fakeAsync(() => {
      const investmentId = 123;
      let errorResponse: any;

      service.pollPaymentAvailability(investmentId).subscribe({
        error: (err) => errorResponse = err
      });

      const req = httpMock.expectOne(`${api}/payments/investment/${investmentId}`);
      req.flush('Forbidden', { status: 403, statusText: 'Forbidden' });

      httpMock.expectNone(`${api}/payments/investment/${investmentId}`); // No more retries
      expect(errorResponse.error).toBe('Forbidden');
    }));
  });

  it('should create order and normalize response', () => {
    const reqBody = { investmentId: 1 };
    service.createOrder(reqBody).subscribe(res => {
      expect(res.success).toBeTrue();
      expect(res.data!.orderId).toBe('order_123');
    });

    const req = httpMock.expectOne(`${api}/payments/create-order`);
    expect(req.request.body).toEqual(reqBody);
    req.flush({ data: { orderId: 'order_123', investmentId: 1, amount: 500, currency: 'INR' } });
  });
});
