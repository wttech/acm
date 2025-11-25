import { expect, Page } from '@playwright/test';
import { Strings } from './lang';
import { readFromCodeEditorAsJson } from './editor';
import { getLabeledValueText } from './labeledValue';

export async function expectCompilationSucceeded(page: Page) {
  await expect(async () => {
    await expect(page.locator('#compilation-status')).toHaveText('Compilation succeeded');
  }).toPass({ timeout: 60000, intervals: [1000, 3000] });
}

export async function expectExecutionProgressBarSucceeded(page: Page) {
  await expect(page.locator('#execution-progress-bar')).toHaveText(/Succeeded after .+/, { timeout: 60000 });
}

export async function expectHealthyStatus(page: Page) {
  await expect(async () => {
    await expect(page.locator('#health-checker-status')).toHaveText('Healthy');
  }).toPass({ timeout: 60000, intervals: [1000, 3000] });
}

export async function expectCodeExecutorStatus(page: Page) {
  await expect(async () => {
    await expect(page.locator('#code-executor-status')).toHaveText('Idle');
  }).toPass({ timeout: 60000, intervals: [1000, 3000] });
}

export function expectToHaveMultilineText(actual: string, expected: string) {
  const lines = Strings.dedent(expected).trim().split('\n').map(line => line.trim());
  for (const line of lines) {
    expect(actual).toContain(line);
  }
}

export function expectToMatchTimestamp(actual: string) {
  expect(actual).toMatch(/\d+ \w+ \d{4} at \d+:\d+( \(.+\))?/);
}

export async function expectExecutionDetails(page: Page, options: { user?: string; status?: string } = {}) {
  const { user = 'admin', status = 'succeeded' } = options;
  
  const executionStatus = page.locator('#execution-status');
  const executionId = await getLabeledValueText(page, 'ID', executionStatus);
  expect(executionId).toMatch(/^\d{4}\/\d+\/\d+\/\d+\/\d+\//);
  
  const userName = await getLabeledValueText(page, 'User', executionStatus);
  expect(userName).toBe(user);
  
  const statusBadge = await getLabeledValueText(page, 'Status', executionStatus);
  expect(statusBadge.toLowerCase()).toBe(status.toLowerCase());
}

export async function expectExecutionTimings(page: Page) {
  const executionTiming = page.locator('#execution-timing');
  
  const startedAt = await getLabeledValueText(page, 'Started At', executionTiming);
  expectToMatchTimestamp(startedAt);
  
  const duration = await getLabeledValueText(page, 'Duration', executionTiming);
  expect(duration).toMatch(/\d+ ms \(\d+ seconds?\)/);
  
  const endedAt = await getLabeledValueText(page, 'Ended At', executionTiming);
  expectToMatchTimestamp(endedAt);
}

export async function expectExecutionInputs(page: Page, expectedInputs: Record<string, any>) {
  const inputs = await readFromCodeEditorAsJson<Record<string, any>>(page, 'Execution Inputs JSON');
  expect(inputs).toEqual(expectedInputs);
}

export async function expectExecutionOutputs(page: Page, expectedOutputs: Array<any>) {
  const outputs = await readFromCodeEditorAsJson<Array<any>>(page, 'Execution Outputs JSON');
  expect(outputs).toHaveLength(expectedOutputs.length);
  expectedOutputs.forEach((expected, index) => {
    expect(outputs[index]).toMatchObject(expected);
  });
}

export async function expectOutputTexts(page: Page, expectedTexts: string[]) {
  for (const text of expectedTexts) {
    await expect(page.getByText(text)).toBeVisible();
  }
}

export async function expectOutputFileDownload(page: Page, buttonName: string, filenamePattern: RegExp) {
  const downloadPromise = page.waitForEvent('download');
  await page.getByRole('button', { name: buttonName }).click();
  const download = await downloadPromise;
  expect(download.suggestedFilename()).toMatch(filenamePattern);
}