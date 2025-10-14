import { test, expect } from '@playwright/test';
  
test('Console executes script', async ({ page }) => {
    await page.goto('/acm');

    await page.getByRole('button', { name: 'Console' }).click();
    await page.getByRole('button', { name: 'Execute' }).click();

    await page.getByText('Output').click();
    await page.getByText('Hello World!').waitFor({ timeout: 15000 });
});
