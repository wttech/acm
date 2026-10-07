import * as vscode from 'vscode';
import { AcmHttpError } from '@acm/shared';

export function showError(error: unknown): void {
  const message = `ACM: ${error instanceof Error ? error.message : String(error)}`;
  if (error instanceof AcmHttpError && error.httpStatus === 401) {
    vscode.window.showErrorMessage(message, 'Set Credentials').then((action) => {
      if (action) vscode.commands.executeCommand('acm.setCredentials');
    });
  } else {
    vscode.window.showErrorMessage(message);
  }
}

/** Registers a command whose failures are shown to the user instead of being lost. */
export function registerCommand(command: string, handler: (...args: never[]) => unknown): vscode.Disposable {
  return vscode.commands.registerCommand(command, async (...args: unknown[]) => {
    try {
      await handler(...(args as never[]));
    } catch (error) {
      showError(error);
    }
  });
}
