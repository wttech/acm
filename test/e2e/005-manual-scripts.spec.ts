import { test, expect } from '@playwright/test';
import { expectExecutionProgressBarSucceeded, expectToMatchTimestamp } from './utils/expect';
import { readFromCodeEditor, readFromCodeEditorAsJson } from './utils/editor';
import { getLabeledValueText } from './utils/labeledValue';

test.describe('Manual Scripts', () => {
  test('Execute CSV Generation With I/O', async ({ page }) => {
    await page.goto('/acm');
    await page.getByRole('button', { name: 'Scripts' }).click();
    await page.getByText('example/ACME-203_output-csv').click();
    await page.getByRole('button', { name: 'Execute' }).click();

    await page.getByRole('textbox', { name: 'Input \'count\' Users to' }).fill('5000');
    await page.getByRole('textbox', { name: 'Input \'firstNames\' First names' }).fill('John\nJane\nJack\nAlice\nBob\nRobert');
    await page.getByRole('textbox', { name: 'Input \'lastNames\' Last names' }).fill('Doe\nSmith\nBrown\nJohnson\nWhite\nJordan');

    await page.getByRole('button', { name: 'Start' }).click();
    await expectExecutionProgressBarSucceeded(page);

    const output = await readFromCodeEditor(page, 'Execution Output');
    expect(output).toContain('[SUCCESS] Users CSV report generation ended successfully');

    await page.getByRole('button', { name: 'Review' }).click();
    await expect(page.getByText('Processed 5000 user(s)')).toBeVisible();
    await page.getByRole('tab', { name: 'Files' }).click();

    const downloadArchivePromise = page.waitForEvent('download');
    await page.getByRole('button', { name: 'Download Archive' }).click();
    const downloadArchive = await downloadArchivePromise;
    expect(downloadArchive.suggestedFilename()).toMatch(/\.(zip)$/);

    await page.getByRole('button', { name: 'Review' }).click();
    await page.getByRole('tab', { name: 'Files' }).click();
    const downloadConsolePromise = page.waitForEvent('download');
    await page.getByRole('button', { name: 'Download Console' }).click();
    const downloadConsole = await downloadConsolePromise;
    expect(downloadConsole.suggestedFilename()).toMatch(/\.console\.log$/);

    await page.getByRole('button', { name: 'Review' }).click();
    await page.getByRole('tab', { name: 'Files' }).click();
    const downloadReportPromise = page.waitForEvent('download');
    await page.getByRole('button', { name: 'Download Report' }).click();
    const downloadReport = await downloadReportPromise;
    expect(downloadReport.suggestedFilename()).toMatch(/\.csv$/);

  
    await page.getByRole('tab', { name: 'Details' }).click();
    
    const executionStatus = page.locator('#execution-status');
    const executionId = await getLabeledValueText(page, 'ID', executionStatus);
    expect(executionId).toMatch(/^2025\/\d+\/\d+\/\d+\/\d+\//);
    
    await expect(page.getByText('admin')).toBeVisible();
    await expect(executionStatus.getByRole('presentation').filter({ hasText: 'succeeded' })).toBeVisible();
    
    const executionTiming = page.locator('#execution-timing');
    const startedAt = await getLabeledValueText(page, 'Started At', executionTiming);
    expectToMatchTimestamp(startedAt);
    
    const duration = await getLabeledValueText(page, 'Duration', executionTiming);
    expect(duration).toMatch(/\d+ ms \(\d+ seconds?\)/);
    
    const endedAt = await getLabeledValueText(page, 'Ended At', executionTiming);
    expectToMatchTimestamp(endedAt);

    const inputs = await readFromCodeEditorAsJson<Record<string, any>>(page, 'Execution Inputs JSON');
    expect(inputs).toEqual({
      count: 5000,
      firstNames: 'John\nJane\nJack\nAlice\nBob\nRobert',
      lastNames: 'Doe\nSmith\nBrown\nJohnson\nWhite\nJordan',
    });

    const outputs = await readFromCodeEditorAsJson<Array<any>>(page, 'Execution Outputs JSON');
    expect(outputs).toHaveLength(2);
    expect(outputs[0]).toMatchObject({
      type: 'FILE',
      name: 'report',
      label: 'Report',
      downloadName: 'report.csv',
    });
    expect(outputs[1]).toMatchObject({
      type: 'TEXT',
      name: 'summary',
      value: 'Processed 5000 user(s)',
    });
  });
});
