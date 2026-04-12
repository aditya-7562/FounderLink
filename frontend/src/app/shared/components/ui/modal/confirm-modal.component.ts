import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ConfirmService } from '../../../../core/services/confirm.service';

@Component({
  selector: 'app-confirm-modal',
  imports: [CommonModule],
  templateUrl: './confirm-modal.component.html',
  styleUrl: './confirm-modal.component.css'
})
export class ConfirmModalComponent {
  constructor(public confirmService: ConfirmService) {}

  onConfirm() {
    this.confirmService.respond(true);
  }

  onCancel() {
    this.confirmService.respond(false);
  }
}
