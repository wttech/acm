import { test, expect } from '@playwright/test';
import { expectCompilationSucceeded, expectExecutionProgressBarSucceeded } from './utils/expect';
import { readFromCodeEditor, writeToCodeEditor } from './utils/editor';
import { newAemContext } from './utils/context';
import { attachScreenshot } from './utils/page';

test.describe('Tool Access', () => {
  test('Admin user has full access', async ({ page }, testInfo) => {
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

    await attachScreenshot(page, testInfo, 'Admin Full Access');
  });

  test('Setup test user and verify limited access', async ({ page, browser }, testInfo) => {
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
              deny { path = "/apps/acm/feature/execution/list"; permissions = ["jcr:read"] }
              allow { path = "/apps/acm/feature/execution/view"; permissions = ["jcr:read"] }
              
              allow { pathStrict = "/conf/acm/settings/script"; permissions = ["jcr:read"] }
              allow { pathStrict = "/conf/acm/settings/script/manual"; permissions = ["jcr:read"] }
              allow { pathStrict = "/conf/acm/settings/script/manual/example"; permissions = ["jcr:read"] }
              allow { path = "/conf/acm/settings/script/manual/example/ACME-200_hello-world.groovy"; permissions = ["jcr:read"] }

              allow { pathStrict = "/conf/acm/settings/script/automatic"; permissions = ["jcr:read"] }
              allow { pathStrict = "/conf/acm/settings/script/extension"; permissions = ["jcr:read"] }
          }
          def testUser = acl.createUser { id = "acm-test-user"; password = "test1234" }
          acl.addToGroup { authorizable = testUser; group = scriptUsers }
          
          out.success "Setup complete!"
      }
    `);
    await expectCompilationSucceeded(page);
    
    await expect(page.getByRole('button', { name: 'Execute' })).toBeEnabled();
    await page.getByRole('button', { name: 'Execute' }).click();
    await expect(page.getByRole('tab', { name: 'Output' })).toHaveAttribute('aria-selected', 'true');
    await expectExecutionProgressBarSucceeded(page);
    
    const output = await readFromCodeEditor(page, 'Console Output');
    expect(output).toContain('Setup complete!');

    await newAemContext(browser, 'acm-test-user', 'test1234', async (testUserPage) => {
      await testUserPage.goto('/acm');
      const title = testUserPage.locator('.granite-title', { hasText: 'Content Manager' });
      await expect(title).toBeVisible();

      await expect(testUserPage.getByRole('button', { name: 'Scripts' })).toBeVisible();

      await expect(testUserPage.getByRole('button', { name: 'Console' })).not.toBeVisible();
      await expect(testUserPage.getByRole('button', { name: 'Snippets' })).not.toBeVisible();
      await expect(testUserPage.getByRole('button', { name: 'History' })).not.toBeVisible();
      await expect(testUserPage.getByRole('button', { name: 'Maintenance' })).not.toBeVisible();
      
      await attachScreenshot(testUserPage, testInfo, 'Test User Access - Dashboard');

      await testUserPage.getByRole('button', { name: 'Scripts' }).click();
      await expect(testUserPage).toHaveURL(/\/acm#\/scripts/);

      const grid = testUserPage.locator('[role="grid"][aria-label="Script list (manual)"]');
      await expect(grid).toBeVisible();
      
      const rows = grid.locator('[role="row"]');
      await expect(rows).toHaveCount(2);
      
      const scriptRow = rows.nth(1);
      await expect(scriptRow.locator('[role="rowheader"]')).toContainText('example/ACME-200_hello-world');

      await attachScreenshot(testUserPage, testInfo, 'Test User Access - Script List');

      // Check if routing blocks access to other tools
      await testUserPage.goto('/acm#/console');
      await expect(testUserPage.getByRole('button', { name: 'Console' })).not.toBeVisible();

      await testUserPage.goto('/acm#/maintenance');
      await expect(testUserPage.getByRole('button', { name: 'Maintenance' })).not.toBeVisible();
    });
  });
});
