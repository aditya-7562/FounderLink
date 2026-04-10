import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';
import { PaymentService } from '../../core/services/payment.service';
import { InvestmentService } from '../../core/services/investment.service';
import { StartupService } from '../../core/services/startup.service';
import { environment } from '../../../environments/environment';
import { InvestmentResponse, InvestmentStatus, PaymentResponse, PaymentStatus, CreateOrderResponse } from '../../models';

interface InvestmentWithPayment {
  investment: InvestmentResponse;
  payment: PaymentResponse | null;
  paymentLoading: boolean;
  paymentError: string;
}

@Component({
  selector: 'app-payments',
  imports: [CommonModule],
  templateUrl: './payments.html',
  styleUrl: './payments.css'
})
export class PaymentsComponent implements OnInit {
  private readonly razorpayKey = environment.razorpayKey || 'rzp_test_YOUR_KEY_HERE';

  items = signal<InvestmentWithPayment[]>([]);
  loading = signal(true);
  errorMsg = signal('');
  successMsg = signal('');
  processingId: number | null = null;
  startupNames = signal<Map<number, string>>(new Map());

  constructor(
    public authService: AuthService,
    private investmentService: InvestmentService,
    private paymentService: PaymentService,
    private startupService: StartupService
  ) {}

  ngOnInit(): void {
    this.loadPortfolio();

    this.startupService.getAll({ page: 0, size: 50, sort: 'createdAt,desc' }).subscribe({
      next: env => {
        const map = new Map<number, string>();
        env.data?.content.forEach(s => map.set(s.id, s.name));
        this.startupNames.set(map);
      }
    });
  }

  loadPortfolio(): void {
    this.loading.set(true);
    this.investmentService.getMyPortfolio({ page: 0, size: 50, sort: 'createdAt,desc' }).subscribe({
      next: env => {
        const all = env.data?.content ?? [];
        // Only show investments that require payment action
        const relevant = all.filter(i =>
          i.status === 'APPROVED' || i.status === 'COMPLETED' || i.status === 'PAYMENT_FAILED'
        );
        const mapped: InvestmentWithPayment[] = relevant.map(inv => ({
          investment: inv, payment: null, paymentLoading: true, paymentError: ''
        }));
        this.items.set(mapped);
        this.loading.set(false);
        // Fetch payment status for each
        mapped.forEach((item, idx) => this.fetchPayment(item.investment, idx));
      },
      error: env => {
        this.errorMsg.set(env.error ?? 'Failed to load investments.');
        this.loading.set(false);
      }
    });
  }

  fetchPayment(investment: InvestmentResponse, idx: number): void {
    this.paymentService.getPaymentByInvestment(investment.id).subscribe({
      next: env => {
        this.items.update(list => {
          const copy = [...list];
          copy[idx] = { ...copy[idx], payment: env.data, paymentLoading: false };
          return copy;
        });
      },
      error: () => {
        // Payment row may not exist yet — poll if investment is APPROVED
        if (investment.status === 'APPROVED') {
          this.pollPayment(investment, idx);
        } else {
          this.items.update(list => {
            const copy = [...list];
            copy[idx] = { ...copy[idx], paymentLoading: false, paymentError: 'Payment record not found.' };
            return copy;
          });
        }
      }
    });
  }

  pollPayment(investment: InvestmentResponse, idx: number): void {
    this.paymentService.pollPaymentAvailability(investment.id).subscribe({
      next: env => {
        this.items.update(list => {
          const copy = [...list];
          copy[idx] = { ...copy[idx], payment: env.data, paymentLoading: false };
          return copy;
        });
      },
      error: () => {
        this.items.update(list => {
          const copy = [...list];
          copy[idx] = {
            ...copy[idx],
            paymentLoading: false,
            paymentError: 'Payment setup is taking longer than expected. Please refresh or try again later.'
          };
          return copy;
        });
      }
    });
  }

  startPayment(item: InvestmentWithPayment): void {
    if (this.processingId !== null) return;
    this.processingId = item.investment.id;
    this.errorMsg.set('');

    this.paymentService.createOrder({ investmentId: item.investment.id }).subscribe({
      next: env => {
        const orderData = env.data!;
        this.processingId = null;
        this.openRazorpay(orderData, item.investment.id);
      },
      error: env => {
        this.processingId = null;
        this.errorMsg.set(env.error ?? 'Failed to create payment order.');
      }
    });
  }

  private openRazorpay(orderData: CreateOrderResponse, investmentId: number): void {
    if (!(window as any).Razorpay) {
      this.errorMsg.set('Razorpay SDK not loaded. Please refresh and try again.');
      return;
    }

    const options = {
      key: this.razorpayKey,
      amount: orderData.amount,
      currency: orderData.currency,
      order_id: orderData.orderId,   // fixed: orderId not razorpayOrderId
      handler: (response: any) => this.confirmPayment(response, investmentId),
      prefill: { email: this.authService.email() },
      theme: { color: '#6366f1' }
    };

    const rzp = new (window as any).Razorpay(options);
    rzp.open();
  }

  private confirmPayment(response: any, investmentId: number): void {
    this.paymentService.confirmPayment({
      razorpayOrderId:   response.razorpay_order_id,
      razorpayPaymentId: response.razorpay_payment_id,
      razorpaySignature: response.razorpay_signature
    }).subscribe({
      next: () => {
        this.successMsg.set('Payment confirmed successfully! Investment is being finalized.');
        setTimeout(() => this.loadPortfolio(), 2000);
      },
      error: env => {
        this.errorMsg.set(env.error ?? 'Payment confirmation failed.');
      }
    });
  }

  paymentStatusClass(status: PaymentStatus | undefined): string {
    if (!status) return 'badge-gray';
    return status === 'SUCCESS'  ? 'badge-success'
         : status === 'INITIATED'? 'badge-info'
         : status === 'PENDING'  ? 'badge-warning'
         : 'badge-danger';
  }

  paymentStatusLabel(status: PaymentStatus | undefined): string {
    if (!status) return 'Awaiting';
    const labels: Record<PaymentStatus, string> = {
      PENDING: 'Ready for Payment', INITIATED: 'Checkout Started',
      SUCCESS: 'Paid', FAILED: 'Payment Failed'
    };
    return labels[status] ?? status;
  }

  canPay(item: InvestmentWithPayment): boolean {
    if (this.processingId !== null) return false;
    const ps = item.payment?.status;
    return item.investment.status === 'APPROVED' && (ps === 'PENDING' || ps === 'INITIATED' || ps === 'FAILED');
  }

  payButtonLabel(item: InvestmentWithPayment): string {
    const ps = item.payment?.status;
    return ps === 'INITIATED' ? 'Resume Payment' : ps === 'FAILED' ? 'Retry Payment' : 'Pay Now';
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(amount);
  }
}
