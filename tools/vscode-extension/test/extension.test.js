const assert = require('node:assert');
const { spawnSync } = require('node:child_process');
const path = require('node:path');
const vscode = require('vscode');

suite('Extension', () => {
  suiteSetup(async () => {
    await vscode.extensions.getExtension('wppes.acm').activate();
  });

  test('bundles a runnable MCP server', () => {
    const server = path.join(vscode.extensions.getExtension('wppes.acm').extensionPath, 'dist', 'mcp-server.mjs');
    const result = spawnSync(process.execPath, [server], {
      env: { ELECTRON_RUN_AS_NODE: '1' },
      encoding: 'utf8',
      timeout: 20000,
    });
    // Without configuration the server stops right after loading all its modules.
    assert.match(result.stderr, /AEM_BASE_URL is required/);
  });

  test('registers commands', async () => {
    const commands = await vscode.commands.getCommands(true);
    for (const command of ['acm.run', 'acm.validate', 'acm.selectInstance', 'acm.newScript']) {
      assert.ok(commands.includes(command), command);
    }
  });

  test('completes script variables', async () => {
    const document = await vscode.workspace.openTextDocument({ language: 'groovy', content: '' });
    const list = await vscode.commands.executeCommand(
      'vscode.executeCompletionItemProvider',
      document.uri,
      new vscode.Position(0, 0),
    );
    const labels = list.items.map((item) => (typeof item.label === 'string' ? item.label : item.label.label));
    assert.ok(labels.includes('repo'));
    assert.ok(labels.includes('doRun'));
  });
});
