import { test, expect } from '@playwright/test';
import { expectExecutionProgressBarSucceeded } from './utils/expect';
import { readFromCodeEditor } from './utils/editor';

test.describe('Manual Scripts', () => {
  test('Execute CSV Generation With Inputs And Outputs', async ({ page }) => {
    await page.goto('/acm');
    await page.getByRole('button', { name: 'Scripts' }).click();
    await page.getByText('example/ACME-203_output-csv').click();
    await page.getByRole('button', { name: 'Execute' }).click();

    await page.getByRole('textbox', { name: 'Input \'count\' Users to' }).fill('5000');
    await page.getByRole('textbox', { name: 'Input \'firstNames\' First names' }).fill('John\nJane\nJack\nAlice\nBob\nRobert');
    await page.getByRole('textbox', { name: 'Input \'lastNames\' Last names' }).fill('Doe\nSmith\nBrown\nJohnson\nWhite\nJordan');

    await page.getByRole('button', { name: 'Start' }).click();
    await expectExecutionProgressBarSucceeded(page);

    const output = await readFromCodeEditor(page);
    expect(output).toContain('[SUCCESS] Users CSV report generation ended successfully');

    await page.getByRole('button', { name: 'Review' }).click();
    await expect(page.getByText('Processed 5000 user(s)')).toBeVisible();
    await page.getByText('Files').click();

    const downloadArchivePromise = page.waitForEvent('download');
    await page.getByRole('button', { name: 'Download Archive' }).click();
    const downloadArchive = await downloadArchivePromise;
    expect(downloadArchive.suggestedFilename()).toMatch(/\.(zip|tar\.gz)$/);

    const downloadConsolePromise = page.waitForEvent('download');
    await page.getByRole('button', { name: 'Download Console' }).click();
    const downloadConsole = await downloadConsolePromise;
    expect(downloadConsole.suggestedFilename()).toMatch(/\.txt$/);

    const downloadReportPromise = page.waitForEvent('download');
    await page.getByRole('button', { name: 'Download Report' }).click();
    const downloadReport = await downloadReportPromise;
    expect(downloadReport.suggestedFilename()).toMatch(/\.csv$/);

    await page.getByRole('button', { name: 'Close' }).click();
  });
});
