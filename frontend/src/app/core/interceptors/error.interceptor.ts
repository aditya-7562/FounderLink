import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { NotificationService } from '../services/notification.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const notificationService = inject(NotificationService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // Allow auth interceptor to handle 401 specifically
      if (error.status !== 401) {
        let title = 'Error';
        let message = 'An unexpected error occurred.';

        if (error.error instanceof ErrorEvent) {
          // Client-side error
          title = 'Client Error';
          message = error.error.message;
        } else {
          // Server-side error
          title = `Server Error (${error.status})`;
          if (error.status === 403) {
            title = 'Forbidden';
            message = "You don't have permission to perform this action.";
          } else if (error.status === 404) {
            title = 'Not Found';
            message = 'The requested resource could not be found.';
          } else if (error.error) {
            message = error.error.message || error.error.error || error.message || message;
          } else {
            message = error.message || message;
          }
        }
        
        notificationService.showUIError(title, message);
      }
      
      return throwError(() => error);
    })
  );
};
