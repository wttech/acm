import { test, expect } from '@playwright/test';
import { expectCompilationSucceeded, expectExecutionProgressBarSucceeded, expectToHaveMultilineText } from './utils/expect'
import { readFromCodeEditor, writeToCodeEditor } from './utils/editor';
import { attachScreenshot } from './utils/page';
  
test.describe('Console', () => {
  test('Executes script', async ({ page }, testInfo) => {
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

    await expect(page.getByRole('tab', { name: 'Output' })).toHaveAttribute('aria-selected', 'true');
    await expectExecutionProgressBarSucceeded(page);
    
    const output = await readFromCodeEditor(page, 'Console Output');
    expectToHaveMultilineText(output, `
        Hello World!
    `);
    await attachScreenshot(page, testInfo, 'Console Output');
  });
});

