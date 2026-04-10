import { CommonModule } from '@angular/common';
import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-pagination-controls',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './pagination-controls.html',
  styleUrl: './pagination-controls.css'
})
export class PaginationControlsComponent {
  page = input(0);
  totalPages = input(0);
  totalElements = input(0);
  label = input('results');

  previous = output<void>();
  next = output<void>();

  get currentPageLabel(): number {
    return this.totalPages() === 0 ? 0 : this.page() + 1;
  }

  get canGoPrevious(): boolean {
    return this.page() > 0;
  }

  get canGoNext(): boolean {
    return this.totalPages() > 0 && this.page() < this.totalPages() - 1;
  }
}
