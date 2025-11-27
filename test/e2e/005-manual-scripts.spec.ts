import { test, expect } from '@playwright/test';
import {
  expectExecutionProgressBarSucceeded,
  expectExecutionDetails,
  expectExecutionTimings,
  expectExecutionInputs,
  expectExecutionOutputs,
  expectOutputTexts,
  expectOutputFileDownload,
} from './utils/expect';
import { readFromCodeEditor } from './utils/editor';
import { attachScreenshot } from './utils/page';

test.describe('Manual Scripts', () => {
  test('Execute CSV Generation', async ({ page }, testInfo) => {
    await page.goto('/acm');
    await page.getByRole('button', { name: 'Scripts' }).click();
    await expect(page.locator('[role="grid"][aria-label="Script list (manual)"]')).toBeVisible();

    await attachScreenshot(page, testInfo, 'Scripts list');
    await page.getByText('example/ACME-203_output-csv').click();
    await attachScreenshot(page, testInfo, 'Script details');
    await page.getByRole('button', { name: 'Execute' }).click();

    await page.getByRole('textbox', { name: 'Users to' }).fill('5000');
    await page.getByRole('textbox', { name: 'First names' }).fill('John\nJane\nJack\nAlice\nBob\nRobert');
    await page.getByRole('textbox', { name: 'Last names' }).fill('Doe\nSmith\nBrown\nJohnson\nWhite\nJordan');
    await attachScreenshot(page, testInfo, 'Execution inputs filled');

    await page.getByRole('button', { name: 'Start' }).click();
    await expectExecutionProgressBarSucceeded(page);
    await attachScreenshot(page, testInfo, 'Execution progress');

    const output = await readFromCodeEditor(page, 'Execution Output');
    expect(output).toContain('[SUCCESS] Users CSV report generation ended successfully');
    await attachScreenshot(page, testInfo, 'Execution output');

    await page.getByRole('tab', { name: 'Details' }).click();

    await page.waitForTimeout(1000);
    await attachScreenshot(page, testInfo, 'Execution details');
    
    await expectExecutionDetails(page);
    await expectExecutionTimings(page);

    await expectExecutionInputs(page, {
      count: 5000,
      firstNames: 'John\nJane\nJack\nAlice\nBob\nRobert',
      lastNames: 'Doe\nSmith\nBrown\nJohnson\nWhite\nJordan',
    });

    await expectExecutionOutputs(page, [
      {
        type: 'FILE',
        name: 'report',
        label: 'Report',
        downloadName: 'report.csv',
      },
      {
        type: 'TEXT',
        name: 'summary',
        value: 'Processed 5000 user(s)',
      },
    ]);
    await attachScreenshot(page, testInfo, 'Execution outputs');

    await page.getByRole('tab', { name: 'Output' }).click();

    await page.getByRole('button', { name: 'Review' }).click();
    await page.getByRole('tab', { name: 'Texts' }).click();
    await expectOutputTexts(page, ['Processed 5000 user(s)']);
    await attachScreenshot(page, testInfo, 'Output review texts');
    await page.getByTestId('modal').getByRole('button', { name: 'Close' }).click();
    
    await page.getByRole('button', { name: 'Review' }).click();
    await page.getByRole('tab', { name: 'Files' }).click();
    await attachScreenshot(page, testInfo, 'Output review files');
    await expectOutputFileDownload(page, 'Download Archive', /\.(zip)$/);

    await page.getByRole('button', { name: 'Review' }).click();
    await page.getByRole('tab', { name: 'Files' }).click();
    await expectOutputFileDownload(page, 'Download Console', /\.console\.log$/);

    await page.getByRole('button', { name: 'Review' }).click();
    await page.getByRole('tab', { name: 'Files' }).click();
    await expectOutputFileDownload(page, 'Download Report', /\.csv$/);
  });
});
