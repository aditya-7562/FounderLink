import { CanDeactivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { ConfirmService } from '../services/confirm.service';

export interface HasDirtyState {
  isDirty(): boolean;
}

export const unsavedChangesGuard: CanDeactivateFn<HasDirtyState> = (component) => {
  if (component.isDirty && component.isDirty()) {
    const confirmService = inject(ConfirmService);
    return confirmService.confirm('You have unsaved changes! Are you sure you want to leave?', {
      isDestructive: true 
    });
  }
  return true;
};
