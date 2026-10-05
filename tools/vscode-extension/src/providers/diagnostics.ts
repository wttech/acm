import * as vscode from 'vscode';

export function registerDiagnostics(context: vscode.ExtensionContext): void {
  const diagnostics = vscode.languages.createDiagnosticCollection('acm');
  context.subscriptions.push(
    diagnostics,
    vscode.workspace.onDidSaveTextDocument((document) => {
      if (document.languageId !== 'groovy' || !vscode.workspace.getConfiguration('acm').get('validateOnSave')) {
        return;
      }
      // TODO: compile-check via ACM_API.executeCode (mode=parse) and map errors to diagnostics;
      // also warn locally about missing required LIFECYCLE_METHODS.
    }),
    vscode.workspace.onDidCloseTextDocument((document) => diagnostics.delete(document.uri)),
  );
}
