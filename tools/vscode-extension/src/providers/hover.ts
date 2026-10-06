import * as vscode from 'vscode';
import { BINDINGS, LIFECYCLE_METHODS, bindingMembers, type CatalogEntry } from '@acm/shared';
import { GROOVY_SELECTOR } from '../groovy';

const ENTRIES = new Map<string, CatalogEntry>([...LIFECYCLE_METHODS, ...BINDINGS].map((entry) => [entry.name, entry]));

// TODO: Java classes via ACM_API.assistCode.
export function registerHover(context: vscode.ExtensionContext): void {
  context.subscriptions.push(
    vscode.languages.registerHoverProvider(GROOVY_SELECTOR, {
      provideHover(document, position) {
        const range = document.getWordRangeAtPosition(position);
        if (!range) {
          return undefined;
        }
        const word = document.getText(range);
        const receiver = /\b(\w+)\.$/.exec(document.lineAt(position).text.slice(0, range.start.character))?.[1];
        const entry = receiver
          ? bindingMembers(receiver).find((member) => member.name === word)
          : ENTRIES.get(word);
        if (!entry) {
          return undefined;
        }
        const markdown = new vscode.MarkdownString().appendCodeblock(entry.signature, 'groovy').appendMarkdown(entry.docs);
        return new vscode.Hover(markdown, range);
      },
    }),
  );
}
