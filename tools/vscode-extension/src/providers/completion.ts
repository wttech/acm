import * as vscode from 'vscode';
import { BINDINGS, bindingMembers, lifecycleMethodsFor, scriptTypeOfPath, type CatalogEntry } from '@acm/shared';
import { GROOVY_SELECTOR } from '../groovy';

// TODO: merge with dynamic suggestions from ACM_API.assistCode (classes, variables, snippets, JCR paths).
export function registerCompletion(context: vscode.ExtensionContext): void {
  context.subscriptions.push(
    vscode.languages.registerCompletionItemProvider(
      GROOVY_SELECTOR,
      {
        provideCompletionItems(document, position) {
          const prefix = document.lineAt(position).text.slice(0, position.character);
          const receiver = /\b(\w+)\.\w*$/.exec(prefix)?.[1];
          if (receiver) {
            return bindingMembers(receiver).map((entry) => toItem(entry, vscode.CompletionItemKind.Method));
          }
          return [
            ...lifecycleMethodsFor(scriptTypeOfPath(document.uri.path)).map((entry) =>
              toItem(entry, vscode.CompletionItemKind.Snippet),
            ),
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
  if (entry.deprecated) {
    item.tags = [vscode.CompletionItemTag.Deprecated];
  }
  return item;
}
