import { Monaco } from '@monaco-editor/react';
import * as monaco from 'monaco-editor';
import { LANGUAGE_ID } from '../../groovy.ts';
import {MarkdownString} from "monaco-editor/esm/vs/base/common/htmlContent";

export function registerCoreCompletions(instance: Monaco) {
  registerScriptCompletions(instance);
}

/**
 * Completions for class 'groovy.lang.Script' which is the base class for all Groovy scripts.
 */
export function registerScriptCompletions(instance: Monaco) {
  instance.languages.registerCompletionItemProvider(LANGUAGE_ID, {
    provideCompletionItems: (model: monaco.editor.ITextModel, position: monaco.Position) => {
      const modelPosition = model.getWordUntilPosition(position);
      const range = new monaco.Range(position.lineNumber, modelPosition.startColumn, position.lineNumber, modelPosition.endColumn);
      const suggestions: monaco.languages.CompletionItem[] = [
        {
          label: 'println()',
          insertText: 'println()',
          documentation: new MarkdownString('Prints a line to the console.'),
          kind: monaco.languages.CompletionItemKind.Function,
          range,
        },
        {
          label: 'print()',
          kind: monaco.languages.CompletionItemKind.Function,
          insertText: 'print()',
          documentation: new MarkdownString('Prints to the console without a newline.'),
          range,
        },
        {
          label: 'println(Object value)',
          kind: monaco.languages.CompletionItemKind.Function,
          insertText: 'println(${1:value})',
          insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
          documentation: new MarkdownString(`
Prints the given value to the console, followed by a newline.

For example:

\`\`\`groovy
println("Hello, World!")
\`\`\`
`),
          range,
        },
        {
          label: 'printf(String format, Object value)',
          kind: monaco.languages.CompletionItemKind.Function,
          insertText: 'printf(${1:format}, ${2:value})',
          insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
          documentation: new MarkdownString(`
Prints a formatted string to the console.

For example:

\`\`\`groovy
printf("Hello %s!", "John")
\`\`\`
`),
          range,
        },
        {
          label: 'printf(String format, Object[] values)',
          kind: monaco.languages.CompletionItemKind.Function,
          insertText: 'printf(${1:format}, ${2:values})',
          insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
          documentation: new MarkdownString(`
Prints a formatted string with multiple values to the console.

For example:
\`\`\`groovy
printf("Hello %s! You have %d new message(s)!", "John", 5)
\`\`\`
`),
          range,
        }
      ];

      return { suggestions };
    },
  });
}
