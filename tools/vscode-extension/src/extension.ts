import * as vscode from 'vscode';
import { registerCommands } from './commands';
import { initInstances } from './instances';
import { registerMcp } from './mcp';
import { registerCodeLens } from './providers/codelens';
import { registerCompletion } from './providers/completion';
import { registerDiagnostics } from './providers/diagnostics';
import { registerHover } from './providers/hover';
import { registerProjectScripts } from './projectScripts';
import { registerStatus } from './status';
import { registerViews } from './views';

export function activate(context: vscode.ExtensionContext): void {
  initInstances(context);
  registerStatus(context);
  const views = registerViews(context);
  registerProjectScripts(context);
  registerCommands(context, views);
  registerCompletion(context);
  registerHover(context);
  registerCodeLens(context);
  registerDiagnostics(context);
  registerMcp(context);
}

export function deactivate(): void {}
