import * as vscode from 'vscode';
import { getActiveInstance } from './instances';

let item: vscode.StatusBarItem;
let running: string | undefined;

export function registerStatus(context: vscode.ExtensionContext): void {
  item = vscode.window.createStatusBarItem(vscode.StatusBarAlignment.Left, 0);
  context.subscriptions.push(
    item,
    vscode.workspace.onDidChangeConfiguration((event) => {
      if (event.affectsConfiguration('acm')) {
        updateStatus();
      }
    }),
  );
  updateStatus();
  item.show();
}

export function setRunning(executionId: string | undefined): void {
  running = executionId;
  updateStatus();
}

function updateStatus(): void {
  const instance = getActiveInstance();
  vscode.commands.executeCommand('setContext', 'acm.hasInstance', !!instance);
  if (running) {
    item.text = `$(sync~spin) ACM: ${instance?.name ?? ''}`;
    item.tooltip = `Running execution ${running}. Click to abort.`;
    item.command = 'acm.abort';
  } else {
    item.text = `$(server) ACM: ${instance?.name ?? 'no instance'}`;
    item.tooltip = instance ? `${instance.url}${instance.readonly ? ' (read-only)' : ''}. Click to switch.` : 'Select ACM instance';
    item.command = 'acm.selectInstance';
  }
}
