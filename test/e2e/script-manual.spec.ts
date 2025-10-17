import { test, expect } from '@playwright/test';
  
test('Manual script is executed', async ({ page }) => {
    await page.goto('/acm#/scripts?tab=manual');

    // await page.getByText('Executor active');

    // await page.getByText('example/ACME-201_inputs').click();
    // await page.getByRole('button', { name: 'Execute' }).click();
    // await page.getByLabel('Input Groups').getByText('Media').click();
    // await page.getByRole('button', { name: 'Pick a path' }).click();
    // await page.  getByText('we-retail', { exact: true }).dblclick();
    // await page.getByText('en', { exact: true }).dblclick();
    // await page.getByText('people').dblclick();
    // await page.getByText('mens', { exact: true }).dblclick();
    // await page.getByText('men_1.jpg').click();
    // await page.getByRole('button', { name: 'Select' }).click();
    // await page.getByRole('button', { name: 'Start' }).click();

    // await expect(page.locator('text=Succeeded after')).toHaveText(/Succeeded after \d+ ms/);
});
