import { Monaco } from '@monaco-editor/react';
import * as monaco from 'monaco-editor';
import { LANGUAGE_ID } from '../../groovy.ts';

export function registerSelfCodeCompletions(instance: Monaco) {
  instance.languages.registerCompletionItemProvider(LANGUAGE_ID, {
    provideCompletionItems: (model: monaco.editor.ITextModel, position: monaco.Position) => {
      const textUntilPosition = model.getValueInRange({
        startLineNumber: 1,
        startColumn: 1,
        endLineNumber: position.lineNumber,
        endColumn: position.column,
      });

      const word = model.getWordUntilPosition(position);
      const range = new monaco.Range(position.lineNumber, word.startColumn, position.lineNumber, word.endColumn);

      const suggestions: monaco.languages.CompletionItem[] = [];

      // Use regex to find variable declarations
      const variableRegex = /\b(def|var|final|static)\s+(\w+)\s*=\s*.+/g;
      let match;
      while ((match = variableRegex.exec(textUntilPosition)) !== null) {
        suggestions.push({
          label: match[2],
          kind: monaco.languages.CompletionItemKind.Variable,
          insertText: match[2],
          documentation: `Variable: ${match[2]}`,
          range: range,
        });
      }

      // Use regex to find method declarations
      const methodRegex = /\b(\w+)\s+(\w+)\s*\(/g;
      while ((match = methodRegex.exec(textUntilPosition)) !== null) {
        suggestions.push({
          label: match[2],
          kind: monaco.languages.CompletionItemKind.Method,
          insertText: match[2] + '()',
          documentation: `Method: ${match[2]}`,
          range: range,
        });
      }

      // Use regex to find class declarations
      const classRegex = /\b(class|interface|trait)\s+(\w+)/g;
      while ((match = classRegex.exec(textUntilPosition)) !== null) {
        suggestions.push({
          label: match[2],
          kind: monaco.languages.CompletionItemKind.Class,
          insertText: match[2],
          documentation: `Class: ${match[2]}`,
          range: range,
        });
      }

      return { suggestions };
    },
  });
}
