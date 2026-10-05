import * as vscode from 'vscode';
import { BINDINGS, LIFECYCLE_METHODS, type CatalogEntry } from '@acm/shared';
import { GROOVY_SELECTOR } from '../groovy';

const ENTRIES = new Map<string, CatalogEntry>([...LIFECYCLE_METHODS, ...BINDINGS].map((entry) => [entry.name, entry]));

// TODO: hover for inputs.*/outputs.* by receiver, and Java classes via ACM_API.assistCode.
export function registerHover(context: vscode.ExtensionContext): void {
  context.subscriptions.push(
    vscode.languages.registerHoverProvider(GROOVY_SELECTOR, {
      provideHover(document, position) {
        const range = document.getWordRangeAtPosition(position);
        const entry = range && ENTRIES.get(document.getText(range));
        if (!entry) {
          return undefined;
        }
        const markdown = new vscode.MarkdownString().appendCodeblock(entry.signature, 'groovy').appendMarkdown(entry.docs);
        return new vscode.Hover(markdown, range);
      },
    }),
  );
}
