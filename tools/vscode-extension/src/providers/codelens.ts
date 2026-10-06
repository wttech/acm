import * as vscode from 'vscode';
import { GROOVY_SELECTOR } from '../groovy';

const METHOD_PATTERN = /^\s*void\s+doRun\s*\(/;

// Validation runs on save, inputs are asked for on run, so Run is the only lens.
export function registerCodeLens(context: vscode.ExtensionContext): void {
  context.subscriptions.push(
    vscode.languages.registerCodeLensProvider(GROOVY_SELECTOR, {
      provideCodeLenses(document) {
        for (let line = 0; line < document.lineCount; line++) {
          if (METHOD_PATTERN.test(document.lineAt(line).text)) {
            return [new vscode.CodeLens(document.lineAt(line).range, { title: '$(play) Run', command: 'acm.run' })];
          }
        }
        return [];
      },
    }),
  );
}
