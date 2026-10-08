import * as vscode from 'vscode';
import { registerBrowser } from './browser';
import { registerCommands } from './commands';
import { registerInputsHover } from './inputs';
import { initInstances } from './instances';
import { registerMcp } from './mcp';
import { registerCodeLens } from './providers/codelens';
import { registerCompletion } from './providers/completion';
import { registerDiagnostics } from './providers/diagnostics';
import { registerHover } from './providers/hover';
import { registerProjectContent } from './project';
import { registerMock } from './mock';
import { registerStatus } from './status';
import { registerViews } from './views';

export function activate(context: vscode.ExtensionContext): void {
  initInstances(context);
  registerMock(context);
  registerStatus(context);
  const views = registerViews(context);
  registerProjectContent(context);
  registerBrowser(context);
  registerCommands(context, views);
  registerCompletion(context);
  registerHover(context);
  registerInputsHover(context);
  registerCodeLens(context);
  registerDiagnostics(context);
  registerMcp(context);
}

export function deactivate(): void {}
