import { Locator, Page } from '@playwright/test';

/**
 * Gets the value text from a LabeledValue by its label
 * Works with various HTML structures by:
 * 1. Finding any element containing the label text
 * 2. Getting its parent container
 * 3. Finding the first non-empty sibling with the value
 */
export async function getLabeledValueText(page: Page, label: string, scope?: Locator): Promise<string> {
  const container = scope || page;
  
  // Find the label element (could be label, span, or any element with the text)
  const labelElement = container.locator(`:text-is("${label}")`).first();
  
  // Get the parent container (the Field wrapper)
  const fieldContainer = labelElement.locator('..');
  
  // Get all text content from the container and remove the label text
  const fullText = await fieldContainer.textContent() || '';
  const valueText = fullText.replace(label, '').trim();
  
  return valueText;
}
