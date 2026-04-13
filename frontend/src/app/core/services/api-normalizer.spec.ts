import { HttpErrorResponse } from '@angular/common/http';
import * as normalizer from './api-normalizer';

describe('ApiNormalizer', () => {
  
  describe('normalizeWrapped', () => {
    it('should extract data from a wrapped response', () => {
      const response = { message: 'Success', data: { id: 1 } };
      const result = normalizer.normalizeWrapped(response);
      expect(result.success).toBeTrue();
      expect(result.data).toEqual({ id: 1 });
      expect(result.error).toBeNull();
    });

    it('should return null if data is missing', () => {
      const response = { message: 'Success' };
      const result = normalizer.normalizeWrapped(response as any);
      expect(result.data).toBeNull();
    });
  });

  describe('normalizePlain', () => {
    it('should wrap a plain object', () => {
      const body = { id: 1 };
      const result = normalizer.normalizePlain(body);
      expect(result.success).toBeTrue();
      expect(result.data).toEqual(body);
    });
  });

  describe('normalizeArray', () => {
    it('should wrap an array', () => {
      const body = [{ id: 1 }];
      const result = normalizer.normalizeArray(body);
      expect(result.data).toEqual(body);
    });

    it('should return empty array if body is not an array', () => {
      const result = normalizer.normalizeArray(null as any);
      expect(result.data).toEqual([]);
    });
  });

  describe('normalizeCollection', () => {
    it('should handle Pattern A (Wrapped with success boolean)', () => {
      const body = { success: true, data: { content: [1, 2], totalElements: 2 } };
      const result = normalizer.normalizeCollection<number>(body);
      expect(result.success).toBeTrue();
      expect(result.data!.content).toEqual([1, 2]);
      expect(result.data!.totalElements).toBe(2);
    });

    it('should handle Pattern B (Wrapped with message and data)', () => {
      const body = { message: 'OK', data: { content: [1] } };
      const result = normalizer.normalizeCollection<number>(body);
      expect(result.success).toBeTrue();
      expect(result.data!.content).toEqual([1]);
    });

    it('should handle plain array', () => {
      const body = [1, 2, 3];
      const result = normalizer.normalizeCollection<number>(body);
      expect(result.data!.content).toEqual([1, 2, 3]);
      expect(result.data!.totalElements).toBe(3);
      expect(result.data!.totalPages).toBe(1);
    });

    it('should handle plain page object', () => {
      const body = { content: [1], totalElements: 10, size: 1, page: 0 };
      const result = normalizer.normalizeCollection<number>(body);
      expect(result.data!.content).toEqual([1]);
      expect(result.data!.totalPages).toBe(10);
    });

    it('should return empty page for malformed data', () => {
      const result = normalizer.normalizeCollection(null);
      expect(result.data!.content).toEqual([]);
      expect(result.data!.totalElements).toBe(0);
    });
  });

  describe('normalizeError', () => {
    it('should extract message from string body', () => {
      const error = new HttpErrorResponse({ error: 'System error', status: 500 });
      const result = normalizer.normalizeError(error);
      expect(result.success).toBeFalse();
      expect(result.error).toBe('System error');
    });

    it('should handle user-service pattern { message, error }', () => {
      const error = new HttpErrorResponse({ 
        error: { message: 'VALIDATION_ERROR', error: 'Invalid name' },
        status: 400 
      });
      const result = normalizer.normalizeError(error);
      expect(result.error).toBe('Invalid name');
    });

    it('should handle generic service pattern { message }', () => {
      const error = new HttpErrorResponse({ 
        error: { message: 'Access denied' },
        status: 403 
      });
      const result = normalizer.normalizeError(error);
      expect(result.error).toBe('Access denied');
    });

    it('should handle validation field map pattern', () => {
      const error = new HttpErrorResponse({ 
        error: { name: 'Too short', email: 'Invalid' },
        status: 400 
      });
      const result = normalizer.normalizeError(error);
      expect(result.error).toContain('name: Too short');
      expect(result.error).toContain('email: Invalid');
    });

    it('should fallback to status text if no body exists', () => {
      const error = new HttpErrorResponse({ status: 404, statusText: 'Not Found' });
      const result = normalizer.normalizeError(error);
      expect(result.error).toBe('Http failure response for (unknown url): 404 Not Found');
    });
  });
});
