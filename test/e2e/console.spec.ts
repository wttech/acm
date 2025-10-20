import { test, expect } from '@playwright/test';
import { expectCompilationSucceeded, expectExecutionProgressBarSucceeded, expectOutputToContainLines } from './utils/expect'
import { readFromCodeEditor, writeToCodeEditor } from './utils/editor';
  
test('Console executes script', async ({ page }) => {
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

    await page.getByText('Output').click();
    await expectExecutionProgressBarSucceeded(page);
    
    const output = await readFromCodeEditor(page);
    expectOutputToContainLines(output, `
        Hello World!
    `);
});
