import { test, expect } from '@playwright/test';
import { expectCompilationSucceeded, expectExecutionProgressBarSucceeded } from './utils/expect';
import { readFromCodeEditor, writeToCodeEditor } from './utils/editor';

test.describe.serial('Tool Permissions', () => {
  test('Setup test user with limited access', async ({ page }) => {
    await page.goto('/acm#/console');

    await expectCompilationSucceeded(page);

    await writeToCodeEditor(page, `
      boolean canRun() {
          return conditions.always()
      }

      void doRun() {
          def scriptUsers = acl.createGroup { id = "acm-script-users" }.tap {
              allow { path = "/apps/cq/core/content/nav/tools/acm"; permissions = ["jcr:read"] }
              allow { path = "/apps/acm/gui"; permissions = ["jcr:read"] }
              allow { path = "/apps/acm/api"; permissions = ["jcr:read"] }
              
              allow { path = "/apps/acm/feature/script/list"; permissions = ["jcr:read"] }
              allow { path = "/apps/acm/feature/script/view"; permissions = ["jcr:read"] }
              allow { path = "/apps/acm/feature/execution/list"; permissions = ["jcr:read"] }
              allow { path = "/apps/acm/feature/execution/view"; permissions = ["jcr:read"] }
              
              allow { path = "/conf/acm/settings/script"; permissions = ["jcr:read"] }
          }
          
          def testUser = acl.createUser { 
              id = "acm-test-user"
              password = "test1234"
          }
          
          acl.addToGroup {
              authorizable = testUser
              group = scriptUsers
          }
          
          out.success "Setup complete!"
      }
    `);
    await expectCompilationSucceeded(page);
    
    await expect(page.getByRole('button', { name: 'Execute' })).toBeEnabled();
    await page.getByRole('button', { name: 'Execute' }).click();

    await page.getByText('Output').click();
    await expectExecutionProgressBarSucceeded(page);
    
    const output = await readFromCodeEditor(page);
    expect(output).toContain('Setup complete!');
  });

  test('Test user has limited access', async ({ browser }) => {
    const context = await browser.newContext({
      baseURL: 'http://localhost:5502',
      extraHTTPHeaders: {
        'Authorization': 'Basic ' + btoa('acm-test-user:test1234'),
      },
    });
    const page = await context.newPage();

    try {
      await page.goto('/acm');
      const title = page.locator('.granite-title', { hasText: 'Content Manager' });
      await expect(title).toBeVisible();

      await expect(page.getByRole('button', { name: 'Scripts' })).toBeVisible();
      await expect(page.getByRole('button', { name: 'History' })).toBeVisible();

      await expect(page.getByRole('button', { name: 'Console' })).not.toBeVisible();
      await expect(page.getByRole('button', { name: 'Snippets' })).not.toBeVisible();
      await expect(page.getByRole('button', { name: 'Maintenance' })).not.toBeVisible();

      await page.getByRole('button', { name: 'Scripts' }).click();
      await expect(page).toHaveURL(/\/acm#\/scripts/);

      await page.getByRole('button', { name: 'History' }).click();
      await expect(page).toHaveURL(/\/acm#\/history/);

      await page.goto('/acm#/console');
      await expect(page.getByRole('button', { name: 'Console' })).not.toBeVisible();

      await page.goto('/acm#/maintenance');
      await expect(page.getByRole('button', { name: 'Maintenance' })).not.toBeVisible();

    } finally {
      await context.close();
    }
  });

  test('Admin user has full access', async ({ page }) => {
    await page.goto('/acm');

    await expect(page.getByRole('button', { name: 'Console' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Scripts' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Snippets' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'History' })).toBeVisible();
    
    const maintenanceButton = page.locator('a[href="#/maintenance"] button');
    await expect(maintenanceButton).toBeVisible();

    await page.goto('/acm#/console');
    await expectCompilationSucceeded(page);
    await expect(page.getByRole('button', { name: 'Execute' })).toBeEnabled();
  });
});
