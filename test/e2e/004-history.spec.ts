import { test, expect } from '@playwright/test';
import { readFromCodeEditor } from './utils/editor';

test.describe('History', () => {
  test('Shows executions from console and tool access tests', async ({ page }) => {
    await page.goto('/acm#/history');

    await page.waitForTimeout(3000);

    const grid = page.locator('[role="grid"][aria-label="Executions table"]');
    await expect(grid).toBeVisible();
    
    const rows = grid.locator('[role="row"]');
    await expect(rows.nth(2)).toBeVisible();
    
    const firstRow = rows.nth(1);
    await expect(firstRow.locator('[role="rowheader"]')).toContainText('Console');
    await firstRow.click();
    await page.getByRole('tab', { name: 'Output' }).click();
    const firstOutput = await readFromCodeEditor(page, 'Execution Output');
    expect(firstOutput).toContain('Setup complete!');

    await page.goto('/acm#/history');
    await expect(grid).toBeVisible();

    const secondRow = rows.nth(2);
    await expect(secondRow.locator('[role="rowheader"]')).toContainText('Console');
    await secondRow.click();
    await page.getByRole('tab', { name: 'Output' }).click();
    const secondOutput = await readFromCodeEditor(page, 'Execution Output');
    expect(secondOutput).toContain('Hello World!');
  });
});
