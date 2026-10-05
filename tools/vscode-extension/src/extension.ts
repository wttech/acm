import * as vscode from 'vscode';
import { registerCommands } from './commands';
import { registerCodeLens } from './providers/codelens';
import { registerCompletion } from './providers/completion';
import { registerDiagnostics } from './providers/diagnostics';
import { registerHover } from './providers/hover';
import { registerViews } from './views';

export function activate(context: vscode.ExtensionContext): void {
  registerCommands(context);
  registerCompletion(context);
  registerHover(context);
  registerCodeLens(context);
  registerDiagnostics(context);
  registerViews(context);
}

export function deactivate(): void {}
