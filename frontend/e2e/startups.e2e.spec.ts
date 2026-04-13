import { test, expect } from '@playwright/test';

test.describe('Startup & Investment Journey E2E', () => {
  test.beforeEach(async ({ page }) => {
    // 1. Mock session
    await page.addInitScript(() => {
      window.localStorage.setItem('token', 'e2e-token');
      window.localStorage.setItem('userId', '123');
      window.localStorage.setItem('role', 'ROLE_INVESTOR');
      window.localStorage.setItem('email', 'investor@example.com');
    });

    // 2. Intercept API calls
    await page.route(url => url.toString().includes('founderlink.online'), async route => {
      const url = route.request().url();
      
      if (url.includes('/users/public/stats')) {
        await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ founders: 10, investors: 5, cofounders: 2 }) });
      } else if (url.includes('/startup') && route.request().method() === 'GET') {
        const mockPage = {
          content: [
            { id: 1, name: 'EcoFlow', description: 'Next-gen water tech', industry: 'Energy', stage: 'EARLY_TRACTION', fundingGoal: 5000000, founderId: 456, createdAt: '2026-04-10T10:00:00Z' },
            { id: 2, name: 'HealthAI', description: 'AI diagnostics', industry: 'Healthcare', stage: 'MVP', fundingGoal: 10000000, founderId: 789, createdAt: '2026-04-11T10:00:00Z' }
          ],
          page: 0,
          size: 9,
          totalElements: 2,
          totalPages: 1,
          last: true
        };
        await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ success: true, data: mockPage }) });
      } else if (url.includes('/investments') && route.request().method() === 'POST') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: { id: 99, status: 'PENDING' } })
        });
      } else {
        await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({}) });
      }
    });

    await page.goto('/dashboard/startups');
  });

  test('should display startup list', async ({ page }) => {
    const cards = page.locator('.startup-card');
    await expect(cards).toHaveCount(2);
    await expect(cards.first()).toContainText('EcoFlow');
  });

  test('should open invest modal and submit successfully', async ({ page }) => {
    // Click Invest button on the first card
    const firstCard = page.locator('.startup-card').first();
    const investBtn = firstCard.locator('button:has-text("Invest")');
    await investBtn.click();

    // Modal should be visible
    const modal = page.locator('.modal-card');
    await expect(modal).toBeVisible();
    await expect(modal).toContainText('Invest in EcoFlow');

    // Fill amount and submit
    await modal.locator('input[type="number"]').fill('5000');
    await modal.locator('button:has-text("Submit Investment")').click();

    // Verify success message
    const alert = modal.locator('.alert-success');
    await expect(alert).toBeVisible();
    await expect(alert).toContainText('successfully');
  });

  test('should block investment below minimum', async ({ page }) => {
    const firstCard = page.locator('.startup-card').first();
    await firstCard.locator('button:has-text("Invest")').click();

    const modal = page.locator('.modal-card');
    await modal.locator('input[type="number"]').fill('500'); // Below 1000
    await modal.locator('button:has-text("Submit Investment")').click();

    const errorMsg = modal.locator('.alert-danger');
    await expect(errorMsg).toBeVisible();
    await expect(errorMsg).toContainText('Minimum');
  });
});
