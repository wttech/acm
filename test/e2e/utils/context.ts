import { Browser, Page } from '@playwright/test';
import { authHeader, BASE_URL } from './env';

export async function newAemContext(
  browser: Browser, 
  user: string, 
  password: string,
  callback: (page: Page) => Promise<void>
): Promise<void> {
  const context = await browser.newContext({
    baseURL: BASE_URL,
    extraHTTPHeaders: authHeader(user, password),
  });
  
  const page = await context.newPage();
  
  try {
    await callback(page);
  } finally {
    await page.close();
    await context.close();
  }
}
