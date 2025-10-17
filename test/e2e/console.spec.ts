import { test, expect } from '@playwright/test';
import { writeToCodeEditor, readFromCodeEditor } from './utils'
  
test('Console executes script', async ({ page }) => {
    await page.goto('/acm#/console');
    await page.getByText('Compilation succeeded');

    await writeToCodeEditor(page, 0, `
        boolean canRun() {
            return conditions.always()
        }

        void doRun() {
            println "ACM Console Test Script"
            println "Status: Executing..."
            println "Hello World!"
            println "Status: Completed"
        }
    `);
    await page.getByText('Compilation succeeded');
    
    await expect(page.getByRole('button', { name: 'Execute' })).toBeEnabled();
    await page.getByRole('button', { name: 'Execute' }).click();

    await page.getByText('Output').click();
    await expect(page.locator('text=Succeeded after')).toHaveText(/Succeeded after \d+ ms/);
    
    const outputContent = await readFromCodeEditor(page, 0);
    
    expect(outputContent).toContain('Hello World!');
    expect(outputContent).toContain('ACM Console Test Script');
    expect(outputContent).toContain('Status: Executing...');
    expect(outputContent).toContain('Status: Completed');
});
