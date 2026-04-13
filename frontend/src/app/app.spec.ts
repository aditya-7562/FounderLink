import { TestBed } from '@angular/core/testing';
import { App } from './app';
import { UiNotifierService } from './core/services/ui-notifier.service';
import { ConfirmService } from './core/services/confirm.service';
import { provideRouter } from '@angular/router';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        { provide: UiNotifierService, useValue: { toasts: () => [] } },
        { provide: ConfirmService, useValue: { state: () => ({}) } }
      ]
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should have router-outlet', () => {
    const fixture = TestBed.createComponent(App);
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('router-outlet')).toBeTruthy();
  });
});
