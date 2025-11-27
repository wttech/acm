import { test, expect } from '@playwright/test';
import { readFromCodeEditor } from './utils/editor';
import { attachScreenshot } from './utils/page';

test.describe('Automatic Scripts', () => {

  test('Executions saved in history', async ({ page }, testInfo) => {
    await page.goto('/acm');
    await page.getByRole('button', { name: 'History' }).click();
    
    const grid = page.locator('[role="grid"][aria-label="Executions table"]');
    await expect(grid).toBeVisible();
    const rows = grid.locator('[role="row"]');
  
    await page.getByRole('searchbox', { name: 'Executable' }).fill('example/ACME-20_once');
    await expect(rows.nth(1)).toContainText('Script \'example/ACME-20_once\'');
    await expect(rows.nth(1)).toContainText('succeeded');
    await attachScreenshot(page, testInfo, `Script List filtered by 'ACME-20_once'`);
    
    await page.getByRole('searchbox', { name: 'Executable' }).fill('example/ACME-21_changed');
    await expect(rows.nth(1)).toContainText('Script \'example/ACME-21_changed\'');
    await expect(rows.nth(1)).toContainText('succeeded');
    await attachScreenshot(page, testInfo, `Script List filtered by 'ACME-21_changed'`);
  });
});
