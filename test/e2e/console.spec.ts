import { test, expect } from '@playwright/test';
  
test('Console executes script', async ({ page }) => {
    await page.goto('/acm#console');

    await page.getByText('Compilation succeeded');
    await page.getByRole('button', { name: 'Execute' }).click();

    await page.getByText('Output').click();
    await expect(page.locator('text=Succeeded after')).toHaveText(/Succeeded after \d+ ms/);
    await page.getByText('Hello World!');
});
