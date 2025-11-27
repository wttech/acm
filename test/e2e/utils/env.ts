import { test, Page } from '@playwright/test';

export const testOnEnv = (env: string) => {
  return process.env.AEM_ENV === env ? test : test.skip;
};

export const BASE_URL = 'http://localhost:5502';
export const ADMIN_USER = 'admin';
export const ADMIN_PASSWORD = 'admin';

export const authHeader = (user = ADMIN_USER, password = ADMIN_PASSWORD) => ({
  'Authorization': 'Basic ' + Buffer.from(`${user}:${password}`).toString('base64')
});

export const baseUrlHeaders = () => ({
  'Origin': BASE_URL,
  'Referer': BASE_URL,
});

export const csrfHeader = (token: string) => ({
  'CSRF-Token': token,
});

export const getCsrfToken = async (page: Page): Promise<string> => {
  const tokenResponse = await page.request.get('/libs/granite/csrf/token.json', {
    headers: {
      ...authHeader(),
      ...baseUrlHeaders(),
    },
  });
  const tokenData = await tokenResponse.json();
  return tokenData.token;
};

export const apiHeaders = async (page: Page) => {
  const csrfToken = await getCsrfToken(page);
  return {
    ...authHeader(),
    ...baseUrlHeaders(),
    ...csrfHeader(csrfToken),
  };
};
