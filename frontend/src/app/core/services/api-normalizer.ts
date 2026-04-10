import { HttpErrorResponse } from '@angular/common/http';
import { ApiEnvelope, ApiResponse, PaginatedData } from '../../models';

/** Pattern A: Wrapped { message, data } response */
export function normalizeWrapped<T>(body: ApiResponse<T>): ApiEnvelope<T> {
  return { success: true, data: body.data ?? null, error: null };
}

/** Pattern B: Plain DTO or primitive */
export function normalizePlain<T>(body: T): ApiEnvelope<T> {
  return { success: true, data: body, error: null };
}

/** Pattern C: Plain array */
export function normalizeArray<T>(body: T[]): ApiEnvelope<T[]> {
  return { success: true, data: Array.isArray(body) ? body : [], error: null };
}

function emptyPage<T>(): PaginatedData<T> {
  return {
    content: [],
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
    last: true
  };
}

function toPaginatedData<T>(body: unknown): PaginatedData<T> {
  if (Array.isArray(body)) {
    return {
      content: body as T[],
      page: 0,
      size: body.length || 10,
      totalElements: body.length,
      totalPages: body.length > 0 ? 1 : 0,
      last: true
    };
  }

  if (body && typeof body === 'object') {
    const maybePage = body as Partial<PaginatedData<T>> & { content?: unknown };
    if (Array.isArray(maybePage.content)) {
      const totalElements = Number(maybePage.totalElements ?? maybePage.content.length ?? 0);
      const size = Number(maybePage.size ?? maybePage.content.length ?? 10);
      const totalPages = Number(
        maybePage.totalPages ?? (size > 0 ? Math.ceil(totalElements / size) : 0)
      );
      return {
        content: maybePage.content as T[],
        page: Number(maybePage.page ?? 0),
        size,
        totalElements,
        totalPages,
        last: Boolean(maybePage.last ?? totalPages <= 1)
      };
    }
  }

  return emptyPage<T>();
}

export function normalizeCollection<T>(body: unknown): ApiEnvelope<PaginatedData<T>> {
  if (body && typeof body === 'object') {
    const response = body as {
      success?: boolean;
      data?: unknown;
      error?: string | null;
      message?: string;
    };

    if ('success' in response) {
      return {
        success: Boolean(response.success),
        data: response.data ? toPaginatedData<T>(response.data) : emptyPage<T>(),
        error: response.error ?? null
      };
    }

    if ('message' in response && 'data' in response) {
      return { success: true, data: toPaginatedData<T>(response.data), error: null };
    }
  }

  return { success: true, data: toPaginatedData<T>(body), error: null };
}

/** Pattern D: Empty body (204 logout) */
export function normalizeEmpty(): ApiEnvelope<null> {
  return { success: true, data: null, error: null };
}

/** Patterns E–L: Normalize any backend error to a human-readable string */
export function normalizeError(err: HttpErrorResponse): ApiEnvelope<null> {
  const body = err.error;
  let message: string;

  if (!body) {
    message = err.message || `Request failed with status ${err.status}`;
  } else if (typeof body === 'string') {
    message = body;
  } else if (typeof body === 'object') {
    // Pattern G (user-service): { message: 'VALIDATION_ERROR', error: 'human text' }
    if (body.error && typeof body.error === 'string' && !body.error.match(/^\d{3}$/)) {
      message = body.error;
    }
    // Pattern E (gateway), F (auth-service), H (generic services): { message: 'string' }
    else if (body.message && typeof body.message === 'string') {
      message = body.message;
    }
    // Pattern I (Bean Validation field map): { field: 'msg', field2: 'msg2' }
    else if (!body.status && !body.message && !body.timestamp) {
      const entries = Object.entries(body as Record<string, string>);
      message = entries.map(([k, v]) => `${k}: ${v}`).join('; ');
    }
    // Fallback
    else {
      message = body.message || body.error || err.statusText || `Request failed with status ${err.status}`;
    }
  } else {
    message = `Request failed with status ${err.status}`;
  }

  return { success: false, data: null, error: message };
}
