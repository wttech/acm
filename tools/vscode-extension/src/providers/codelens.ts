import * as vscode from 'vscode';
import { GROOVY_SELECTOR } from '../groovy';

const METHOD_PATTERN = /^\s*(?:void|def|boolean)\s+(doRun|describeRun)\s*\(/;

export function registerCodeLens(context: vscode.ExtensionContext): void {
  context.subscriptions.push(
    vscode.languages.registerCodeLensProvider(GROOVY_SELECTOR, {
      provideCodeLenses(document) {
        const lenses: vscode.CodeLens[] = [];
        for (let line = 0; line < document.lineCount; line++) {
          const method = METHOD_PATTERN.exec(document.lineAt(line).text)?.[1];
          if (!method) {
            continue;
          }
          const range = document.lineAt(line).range;
          if (method === 'doRun') {
            lenses.push(new vscode.CodeLens(range, { title: 'Run', command: 'acm.run' }));
            lenses.push(new vscode.CodeLens(range, { title: 'Validate', command: 'acm.validate' }));
          } else {
            lenses.push(new vscode.CodeLens(range, { title: 'Describe inputs', command: 'acm.describe' }));
          }
        }
        return lenses;
      },
    }),
  );
}
