import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastContainerComponent } from './shared/components/ui/toast/toast.component';
import { ConfirmModalComponent } from './shared/components/ui/modal/confirm-modal.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ToastContainerComponent, ConfirmModalComponent],
  templateUrl: './app.html',
  styles: []
})
export class App {}
