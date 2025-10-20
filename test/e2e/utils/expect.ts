import { expect, Page } from '@playwright/test';

export function expectCompilationSucceeded(page: Page) {
  return page.getByText('Compilation succeeded');
}

export function expectOutputToContainLines(actual: string, expected: string) {
  const lines = expected.trim().split('\n').map(line => line.trim());
  for (const line of lines) {
    expect(actual).toContain(line);
  }
}

export async function expectExecutionProgressBarSucceeded(page: Page) {
  await expect(page.locator('#execution-progress-bar')).toHaveText(/Succeeded after \d+ ms/);
}

export async function expectHealthyStatus(page: Page) {
  await expect(page.locator('#health-status')).toHaveText(/Healthy/, { timeout: 60000 });
}