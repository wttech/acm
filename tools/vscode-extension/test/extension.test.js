const assert = require('node:assert');
const vscode = require('vscode');

suite('Extension', () => {
  suiteSetup(async () => {
    await vscode.extensions.getExtension('wppes.acm').activate();
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
