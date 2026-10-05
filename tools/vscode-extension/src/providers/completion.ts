import * as vscode from 'vscode';
import { BINDINGS, INPUT_METHODS, LIFECYCLE_METHODS, OUTPUT_METHODS, type CatalogEntry } from '@acm/shared';
import { GROOVY_SELECTOR } from '../groovy';

// TODO: merge with dynamic suggestions from ACM_API.assistCode (classes, variables, snippets, JCR paths).
export function registerCompletion(context: vscode.ExtensionContext): void {
  context.subscriptions.push(
    vscode.languages.registerCompletionItemProvider(
      GROOVY_SELECTOR,
      {
        provideCompletionItems(document, position) {
          const prefix = document.lineAt(position).text.slice(0, position.character);
          if (/\binputs\.\w*$/.test(prefix)) {
            return INPUT_METHODS.map((entry) => toItem(entry, vscode.CompletionItemKind.Method));
          }
          if (/\boutputs\.\w*$/.test(prefix)) {
            return OUTPUT_METHODS.map((entry) => toItem(entry, vscode.CompletionItemKind.Method));
          }
          if (/\.\w*$/.test(prefix)) {
            return undefined;
          }
          return [
            ...LIFECYCLE_METHODS.map((entry) => toItem(entry, vscode.CompletionItemKind.Snippet)),
            ...BINDINGS.map((entry) => toItem(entry, vscode.CompletionItemKind.Variable)),
          ];
        },
      },
      '.',
    ),
  );
}

function toItem(entry: CatalogEntry, kind: vscode.CompletionItemKind): vscode.CompletionItem {
  const item = new vscode.CompletionItem(entry.name, kind);
  item.detail = entry.signature;
  item.documentation = new vscode.MarkdownString(entry.docs);
  if (entry.snippet) {
    item.insertText = new vscode.SnippetString(entry.snippet);
  }
  return item;
}
