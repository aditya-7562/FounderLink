import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { StartupsComponent } from './startups';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, provideRouter } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { StartupService } from '../../core/services/startup.service';
import { InvestmentService } from '../../core/services/investment.service';
import { of, throwError } from 'rxjs';
import { By } from '@angular/platform-browser';
import { MOCK_STARTUP, MOCK_STARTUP_LIST } from '../../../testing/mocks/startup.mocks';
import { signal } from '@angular/core';

describe('StartupsComponent', () => {
  let component: StartupsComponent;
  let fixture: ComponentFixture<StartupsComponent>;
  let startupServiceSpy: jasmine.SpyObj<StartupService>;
  let investmentServiceSpy: jasmine.SpyObj<InvestmentService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let router: Router;

  beforeEach(async () => {
    startupServiceSpy = jasmine.createSpyObj('StartupService', ['getAll', 'search']);
    investmentServiceSpy = jasmine.createSpyObj('InvestmentService', ['create']);
    authServiceSpy = jasmine.createSpyObj('AuthService', ['role', 'isLoggedIn']);

    // Default signal values
    authServiceSpy.role.and.returnValue('INVESTOR');
    authServiceSpy.isLoggedIn.and.returnValue(true);

    startupServiceSpy.getAll.and.returnValue(of({
      success: true,
      data: {
        content: MOCK_STARTUP_LIST,
        page: 0,
        size: 9,
        totalElements: 2,
        totalPages: 1,
        last: true
      },
      error: null
    }));

    await TestBed.configureTestingModule({
      imports: [StartupsComponent, CommonModule, FormsModule],
      providers: [
        provideRouter([]),
        { provide: StartupService, useValue: startupServiceSpy },
        { provide: InvestmentService, useValue: investmentServiceSpy },
        { provide: AuthService, useValue: authServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(StartupsComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should create and load startups', () => {
    expect(component).toBeTruthy();
    expect(component.startups().length).toBe(2);
    expect(startupServiceSpy.getAll).toHaveBeenCalled();
  });

  it('should apply filters and call search', fakeAsync(() => {
    startupServiceSpy.search.and.returnValue(of({
      success: true,
      data: { content: [MOCK_STARTUP], page: 0, size: 9, totalElements: 1, totalPages: 1, last: true },
      error: null
    }));

    component.selectedStage = 'MVP';
    component.applyFilters();
    
    expect(startupServiceSpy.search).toHaveBeenCalledWith(jasmine.objectContaining({
      stage: 'MVP'
    }));
    fixture.detectChanges();
    expect(component.startups().length).toBe(1);
  }));

  it('should handle investment submission', fakeAsync(() => {
    investmentServiceSpy.create.and.returnValue(of({ 
      success: true, 
      data: { 
        id: 1, 
        startupId: MOCK_STARTUP.id, 
        investorId: 2, 
        amount: 5000, 
        status: 'PENDING', 
        createdAt: new Date().toISOString() 
      }, 
      error: null 
    }));
    
    component.openInvestModal(MOCK_STARTUP);
    component.investAmount = 5000;
    component.submitInvestment();

    expect(investmentServiceSpy.create).toHaveBeenCalledWith({
      startupId: MOCK_STARTUP.id,
      amount: 5000
    });
    
    tick(); // Wait for observable
    expect(component.investSuccess()).toContain('successfully');
    
    tick(3000); // Wait for modal close setTimeout
    expect(component.investModal()).toBeNull();
  }));

  it('should show error if investment amount is too low', () => {
    component.openInvestModal(MOCK_STARTUP);
    component.investAmount = 500;
    component.submitInvestment();

    expect(component.investError()).toContain('Minimum');
    expect(investmentServiceSpy.create).not.toHaveBeenCalled();
  });

  it('should toggle "Invest" button visibility based on role', () => {
    // Current role is INVESTOR
    fixture.detectChanges();
    // In the template, it's the second button in .sc-actions if isInvestor is true
    let investBtn = fixture.debugElement.queryAll(By.css('.sc-actions .btn-primary'))[0];
    expect(investBtn).toBeTruthy();
    expect(investBtn.nativeElement.textContent).toContain('Invest');

    // Change role to FOUNDER
    authServiceSpy.role.and.returnValue('FOUNDER');
    fixture.detectChanges();
    investBtn = fixture.debugElement.query(By.css('.sc-actions .btn-primary'));
    // If FOUNDER, no .btn-primary should be in sc-actions (it showsView Details link instead)
    expect(investBtn).toBeNull();
  });
});
