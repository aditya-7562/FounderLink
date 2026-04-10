import { CanDeactivateFn } from '@angular/router';

export interface HasDirtyState {
  isDirty(): boolean;
}

export const unsavedChangesGuard: CanDeactivateFn<HasDirtyState> = (component) => {
  if (component.isDirty && component.isDirty()) {
    return confirm('You have unsaved changes! Are you sure you want to leave?');
  }
  return true;
};
