import { test, expect } from '@playwright/test';

test('ACM Content Manager is accessible', async ({ page }) => {
  await page.goto('/acm');

  const title = page.locator('.granite-title', { hasText: 'Content Manager' });
  await expect(title).toBeVisible();
});
