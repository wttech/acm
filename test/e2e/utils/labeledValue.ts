import { Locator, Page } from '@playwright/test';

/**
 * Gets the value text from a LabeledValue by its label
 */
export async function getLabeledValueText(page: Page, label: string, scope?: Locator): Promise<string> {
  const container = scope || page;
  const labeledValue = container.locator('div[class*="LabeledValue"]').filter({
    has: container.locator('span[class*="FieldLabel"]:text-is("' + label + '")')
  });
  const valueSpan = labeledValue.locator('span[class*="Field-field"]').first();
  return await valueSpan.textContent() || '';
}
