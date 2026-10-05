import * as vscode from 'vscode';

// TODO: Executions tree from ACM_API.execution (history + queue), Scripts tree from ACM_API.script.
class EmptyTreeProvider implements vscode.TreeDataProvider<vscode.TreeItem> {
  private readonly changed = new vscode.EventEmitter<void>();
  readonly onDidChangeTreeData = this.changed.event;

  getTreeItem(element: vscode.TreeItem): vscode.TreeItem {
    return element;
  }

  getChildren(): vscode.TreeItem[] {
    return [];
  }
}

export function registerViews(context: vscode.ExtensionContext): void {
  context.subscriptions.push(
    vscode.window.registerTreeDataProvider('acm.executions', new EmptyTreeProvider()),
    vscode.window.registerTreeDataProvider('acm.scripts', new EmptyTreeProvider()),
  );
}
