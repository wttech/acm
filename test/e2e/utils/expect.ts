import { expect, Page } from '@playwright/test';
import { Strings } from './lang';

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