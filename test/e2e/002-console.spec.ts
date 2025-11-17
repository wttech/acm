import { test, expect } from '@playwright/test';
import { expectCompilationSucceeded, expectExecutionProgressBarSucceeded, expectToHaveMultilineText } from './utils/expect'
import { readFromCodeEditor, writeToCodeEditor } from './utils/editor';
  
test.describe('Console', () => {
  test('Console executes script and appears in history', async ({ page }) => {
    await page.goto('/acm#/console');

    await expectCompilationSucceeded(page);

    await writeToCodeEditor(page, `
        boolean canRun() {
            return conditions.always()
        }

        void doRun() {
            println "Hello World!"
        }
    `);
    await expectCompilationSucceeded(page);
    
    await expect(page.getByRole('button', { name: 'Execute' })).toBeEnabled();
    await page.getByRole('button', { name: 'Execute' }).click();

    await page.getByRole('tab', { name: 'Output' }).click();
    await expectExecutionProgressBarSucceeded(page);
    
    const output = await readFromCodeEditor(page);
    expectToHaveMultilineText(output, `
        Hello World!
    `);

    await page.waitForTimeout(1000); // AEM reindexing delay

    await page.goto('/acm#/history');

    const grid = page.locator('[role="grid"][aria-label="Executions table"]');
    await expect(grid).toBeVisible();
    
    const firstRow = grid.locator('[role="row"]').nth(1);
    await expect(firstRow).toBeVisible();
    
    await expect(firstRow.locator('[role="rowheader"]')).toContainText('Console');
    
    await firstRow.click();
    
    await page.getByRole('tab', { name: 'Output' }).click();
    
    const historyOutput = await readFromCodeEditor(page);
    expectToHaveMultilineText(historyOutput, `
        Hello World!
    `);
  });
});

