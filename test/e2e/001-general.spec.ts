import { test, expect } from '@playwright/test';
import { expectCodeExecutorStatus, expectHealthyStatus } from './utils/expect';
import { testOnEnv, apiHeaders } from './utils/env';

test.describe('General', () => {
  test('Tool is accessible', async ({ page }) => {
    await page.goto('/acm');

    const title = page.locator('.granite-title', { hasText: 'Content Manager' });
    await expect(title).toBeVisible();
    
    await page.screenshot();
  });

  testOnEnv('local')('Tool state is reset', async ({ page }) => {
    const clearResponse = await page.request.post('/apps/acm/api/event.json?name=HISTORY_CLEAR', { headers: await apiHeaders(page) });
    expect(clearResponse.ok()).toBeTruthy();
    const clearJson = await clearResponse.json();
    expect(clearJson).toEqual({ status: 200, message: "Event 'HISTORY_CLEAR' dispatched successfully!", data: null });
    await page.waitForTimeout(3000);
    
    const bootResponse = await page.request.post('/apps/acm/api/event.json?name=SCRIPT_SCHEDULER_BOOT', { headers: await apiHeaders(page) });
    expect(bootResponse.ok()).toBeTruthy();
    const bootJson = await bootResponse.json();
    expect(bootJson).toEqual({ status: 200, message: "Event 'SCRIPT_SCHEDULER_BOOT' dispatched successfully!", data: null });
    await page.waitForTimeout(10000);
  });

  test('System is healthy', async ({ page }) => {
    await page.goto('/acm#/maintenance?tab=health-checker');
    
    await expectHealthyStatus(page);
    await page.screenshot();
  });

  test('Code executor is idle', async ({ page }) => {
    await page.goto('/acm#/maintenance?tab=code-executor');
    
    await expectCodeExecutorStatus(page);
    await page.screenshot();
  });

});