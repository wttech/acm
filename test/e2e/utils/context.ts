import { Browser, BrowserContext, Page } from '@playwright/test';

export async function newAemContext(
  browser: Browser, 
  user: string, 
  password: string,
  callback: (page: Page) => Promise<void>
): Promise<void> {
  const context = await browser.newContext({
    baseURL: 'http://localhost:5502',
    extraHTTPHeaders: {
      'Authorization': 'Basic ' + btoa(`${user}:${password}`),
    },
  });
  
  try {
    const page = await context.newPage();
    await callback(page);
  } finally {
    await context.close();
  }
}
