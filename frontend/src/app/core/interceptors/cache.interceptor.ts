import { HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { of, tap } from 'rxjs';

const CACHE_TTL = 30 * 1000; // 30 seconds
const cache = new Map<string, { fetchedAt: number; response: HttpResponse<any> }>();

const isCacheable = (url: string): boolean => {
  return url.includes('/users/public/stats') || url.includes('/startup/search');
};

export const cacheInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.method !== 'GET' || !isCacheable(req.url)) {
    return next(req);
  }

  const cacheKey = req.urlWithParams;
  const cached = cache.get(cacheKey);

  if (cached) {
    const expired = Date.now() - cached.fetchedAt > CACHE_TTL;
    if (!expired) {
      return of(cached.response.clone());
    } else {
      cache.delete(cacheKey);
    }
  }

  return next(req).pipe(
    tap(event => {
      if (event instanceof HttpResponse) {
        cache.set(cacheKey, { fetchedAt: Date.now(), response: event.clone() });
      }
    })
  );
};
