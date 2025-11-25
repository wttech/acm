import { Locator, Page } from '@playwright/test';

/**
 * Gets the value text from a LabeledValue by its label
 */
export async function getLabeledValueText(page: Page, label: string, scope?: Locator): Promise<string> {
  const container = scope || page;
  const labelSpan = container.locator('span[class*="FieldLabel"]:text-is("' + label + '")');
  const valueSpan = labelSpan.locator('..').locator('span[class*="Field-field"]');
  return await valueSpan.textContent() || '';
}
