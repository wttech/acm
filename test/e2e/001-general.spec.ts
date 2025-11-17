import { test, expect } from '@playwright/test';
import { expectCodeExecutorStatus, expectHealthyStatus } from './utils/expect';

test.describe('General', () => {
  test('Tool is accessible', async ({ page }) => {
    await page.goto('/acm');

    const title = page.locator('.granite-title', { hasText: 'Content Manager' });
    await expect(title).toBeVisible();
  });

  test('System is healthy', async ({ page }) => {
    await page.goto('/acm#/maintenance?tab=health-checker');
    
    await expectHealthyStatus(page);
  });

  test('Code executor is idle', async ({ page }) => {
    await page.goto('/acm#/maintenance?tab=code-executor');
    
    await expectCodeExecutorStatus(page);
  });

});