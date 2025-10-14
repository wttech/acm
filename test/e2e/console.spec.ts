import { test, expect } from '@playwright/test';
  
test('Console executes script', async ({ page }) => {
    await page.goto('/acm');

    await page.getByRole('button', { name: 'Console' }).click();
    await page.getByRole('button', { name: 'Execute' }).click();
});
