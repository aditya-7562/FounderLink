import { Injectable, signal } from '@angular/core';

export interface ConfirmState {
  isOpen: boolean;
  message: string;
  title: string;
  confirmLabel: string;
  cancelLabel: string;
  isDestructive: boolean;
}

@Injectable({ providedIn: 'root' })
export class ConfirmService {
  state = signal<ConfirmState>({
    isOpen: false,
    message: '',
    title: 'Confirm',
    confirmLabel: 'OK',
    cancelLabel: 'Cancel',
    isDestructive: false
  });

  private resolveCurrentPromise: ((value: boolean) => void) | null = null;

  confirm(
    message: string, 
    options?: { 
      title?: string, 
      confirmLabel?: string, 
      cancelLabel?: string, 
      isDestructive?: boolean 
    }
  ): Promise<boolean> {
    return new Promise((resolve) => {
      this.resolveCurrentPromise = resolve;
      
      this.state.set({
        isOpen: true,
        message,
        title: options?.title ?? 'Confirm',
        confirmLabel: options?.confirmLabel ?? 'OK',
        cancelLabel: options?.cancelLabel ?? 'Cancel',
        isDestructive: options?.isDestructive ?? false
      });
    });
  }

  respond(result: boolean) {
    if (this.resolveCurrentPromise) {
      this.resolveCurrentPromise(result);
      this.resolveCurrentPromise = null;
    }
    
    this.state.update(current => ({ ...current, isOpen: false }));
  }
}
