import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { LoginComponent } from './login';
import { ReactiveFormsModule } from '@angular/forms';
import { Router, provideRouter } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import { ThemeService } from '../../../core/services/theme.service';
import { of, throwError, Subject } from 'rxjs';
import { By } from '@angular/platform-browser';

import { signal } from '@angular/core';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let userServiceSpy: jasmine.SpyObj<UserService>;
  let router: Router;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['login', 'getHomeRoute']);
    userServiceSpy = jasmine.createSpyObj('UserService', ['getPublicStats']);
    
    authServiceSpy.getHomeRoute.and.returnValue('/dashboard');
    userServiceSpy.getPublicStats.and.returnValue(of({ founders: 1, investors: 2, cofounders: 3 }));

    await TestBed.configureTestingModule({
      imports: [LoginComponent, ReactiveFormsModule],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceSpy },
        { provide: UserService, useValue: userServiceSpy },
        { 
          provide: ThemeService, 
          useValue: { 
            theme: signal('light'), 
            toggleTheme: jasmine.createSpy('toggleTheme') 
          } 
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show validation errors on submit with empty form', () => {
    const submitBtn = fixture.debugElement.query(By.css('button[type="submit"]'));
    submitBtn.nativeElement.click();
    fixture.detectChanges();

    expect(component.form.invalid).toBeTrue();
    expect(component.email.errors?.['required']).toBeTrue();
    expect(component.password.errors?.['required']).toBeTrue();
  });

  it('should show error message on invalid credentials', fakeAsync(() => {
    authServiceSpy.login.and.returnValue(throwError(() => ({ error: { message: 'Invalid creds' } })));
    
    component.form.patchValue({ email: 'test@test.com', password: 'wrong' });
    component.onSubmit();
    
    tick();
    fixture.detectChanges();

    expect(component.errorMsg()).toBe('Invalid creds');
    // The template uses .alert-error
    const errorEl = fixture.debugElement.query(By.css('.alert-error'));
    expect(errorEl).toBeTruthy();
    expect(errorEl.nativeElement.textContent).toContain('Invalid creds');
  }));

  it('should navigate to home on successful login', fakeAsync(() => {
    authServiceSpy.login.and.returnValue(of({ 
      token: 'abc', 
      email: 'p@p.com', 
      role: 'ROLE_INVESTOR', 
      userId: 1 
    }));
    spyOn(router, 'navigateByUrl');
    
    component.form.patchValue({ email: 'test@test.com', password: 'correct' });
    component.onSubmit();
    
    tick();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/dashboard');
  }));

  it('should show loading state during submission', () => {
    const loginSubject = new Subject<any>();
    authServiceSpy.login.and.returnValue(loginSubject.asObservable());
    
    component.form.patchValue({ email: 'test@test.com', password: 'p' });
    component.onSubmit();
    
    // Check loading state while request is pending
    expect(component.loading()).toBeTrue();
    fixture.detectChanges();
    const loadingBtn = fixture.debugElement.query(By.css('.spinner'));
    expect(loadingBtn).toBeTruthy();

    // Complete the request
    loginSubject.next({ token: 'abc', email: 'p@p.com', role: 'INVESTOR', userId: 1 });
    expect(component.loading()).toBeFalse();
  });
});
