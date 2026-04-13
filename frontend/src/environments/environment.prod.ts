export const environment = {
  production: true,
  apiUrl: import.meta.env?.['NG_APP_API_URL'] || '',
  razorpayKey: import.meta.env?.['NG_APP_RAZORPAY_KEY'] || ''
};
