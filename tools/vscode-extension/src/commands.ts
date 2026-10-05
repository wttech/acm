import * as vscode from 'vscode';
import { getInstances, setActiveInstance } from './instances';

// TODO: implement each command, see ROADMAP.md.
const STUBS: Record<string, string> = {
  'acm.run': 'Run Script',
  'acm.runSelection': 'Run Selection',
  'acm.validate': 'Validate Script',
  'acm.describe': 'Describe Inputs',
  'acm.abort': 'Abort Execution',
  'acm.setCredentials': 'Set Credentials',
  'acm.checkConnection': 'Check Connection',
  'acm.refreshExecutions': 'Refresh Executions',
  'acm.refreshScripts': 'Refresh Scripts',
};

export function registerCommands(context: vscode.ExtensionContext): void {
  for (const [command, title] of Object.entries(STUBS)) {
    context.subscriptions.push(
      vscode.commands.registerCommand(command, () =>
        vscode.window.showInformationMessage(`ACM: ${title} is not implemented yet.`),
      ),
    );
  }
  context.subscriptions.push(vscode.commands.registerCommand('acm.selectInstance', selectInstance));
}

async function selectInstance(): Promise<void> {
  const instances = getInstances();
  if (instances.length === 0) {
    await vscode.commands.executeCommand('workbench.action.openSettings', 'acm.instances');
    return;
  }
  const picked = await vscode.window.showQuickPick(
    instances.map((instance) => ({ label: instance.name, description: instance.url })),
    { placeHolder: 'Select ACM instance' },
  );
  if (picked) {
    await setActiveInstance(picked.label);
  }
}
