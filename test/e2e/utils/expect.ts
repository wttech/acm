import { expect, Page } from '@playwright/test';
import { Strings } from './lang';

export async function expectCompilationSucceeded(page: Page) {
  await expect(page.locator('#compilation-status')).toHaveText('Compilation succeeded', { timeout: 60000 });
}

export async function expectExecutionProgressBarSucceeded(page: Page) {
  await expect(page.locator('#execution-progress-bar')).toHaveText(/Succeeded after \d+ ms/, { timeout: 60000 });
}

export async function expectHealthyStatus(page: Page) {
  await expect(page.locator('#health-checker-status')).toHaveText('Healthy', { timeout: 60000 });
}

export async function expectCodeExecutorStatus(page: Page) {
  await expect(page.locator('#code-executor-status')).toHaveText('Idle', { timeout: 60000 });
}

export function expectToHaveMultilineText(actual: string, expected: string) {
  const lines = Strings.dedent(expected).trim().split('\n').map(line => line.trim());
  for (const line of lines) {
    expect(actual).toContain(line);
  }
}