import {Monaco} from '@monaco-editor/react';
import * as monaco from 'monaco-editor';
import {MarkdownString} from 'monaco-editor/esm/vs/base/common/htmlContent';
import {apiRequest} from '../../../api.ts';
import {AssistCodeOutput} from '../../../api.types.ts';
import {LANGUAGE_ID} from '../../groovy.ts';

export function registerAemCodeCompletions(instance: Monaco) {
  registerWordCompletion(instance);
  registerResourceCompletion(instance);
}

function registerWordCompletion(instance: Monaco) {
  instance.languages.registerCompletionItemProvider(LANGUAGE_ID, {
    provideCompletionItems: async (model: monaco.editor.ITextModel, position: monaco.Position): Promise<monaco.languages.CompletionList> => {
      const path = extractPath(model.getLineContent(position.lineNumber), position);
      if (path) {
        // TODO not sure if needed
        return {suggestions: [], incomplete: true};
      }
      let wordText = '';
      const wordAtPosition = model.getWordAtPosition(position);
      if (wordAtPosition) {
        wordText = wordAtPosition.word;
      }

      try {
        const response = await apiRequest<AssistCodeOutput>({
          method: 'GET',
          url: `/apps/acm/api/assist-code.json?type=all&word=${encodeURIComponent(wordText)}`,
          operation: 'Code assistance',
        });
        const assistance = response.data.data;
        const suggestions = (assistance?.suggestions ?? []).map((suggestion) => ({
          label: suggestion.l ?? suggestion.it,
          insertText: suggestion.it ?? suggestion.l,
          insertTextRules: monacoInsertTextRules(suggestion.k),
          kind: monacoKind(suggestion.k),
          detail: suggestion.k,
          documentation: new MarkdownString(suggestion.i),
          range: new monaco.Range(position.lineNumber, wordAtPosition?.startColumn || position.column, position.lineNumber, wordAtPosition?.endColumn || position.column),

          // TODO below does not work, Monaco bug? (we want to prioritize exact class name matches)
          // sortText: sortText(suggestion.v, wordText)
        }));

        return {suggestions: suggestions, incomplete: true};
      } catch (error) {
        console.error('Code assistance error:', error);
        return {suggestions: [], incomplete: true};
      }
    },
  });
}

function registerResourceCompletion(instance: Monaco) {
  instance.languages.registerCompletionItemProvider(LANGUAGE_ID, {
    triggerCharacters: ['/'],

    provideCompletionItems: async (model: monaco.editor.ITextModel, position: monaco.Position): Promise<monaco.languages.CompletionList> => {
      const path = extractPath(model.getLineContent(position.lineNumber), position);
      if (path.length == 0) {
        return {suggestions: [], incomplete: true};
      }
      try {
        const response = await apiRequest<AssistCodeOutput>({
          method: 'GET',
          url: `/apps/acm/api/assist-code.json?type=resource&word=${encodeURIComponent(path)}`,
          operation: 'Code assistance',
        });
        const assistance = response.data.data;
        const suggestions = (assistance?.suggestions ?? []).map((suggestion) => ({
          label: suggestion.it ?? suggestion.l,
          insertText: removePathPrefix(path, suggestion.it ?? suggestion.l), // subtract path prefix
          kind: monacoKind(suggestion.k),
          detail: suggestion.k,
          documentation: new MarkdownString(suggestion.i),
          range: new monaco.Range(position.lineNumber, position.column, position.lineNumber, position.column),
        }));

        return {suggestions: suggestions, incomplete: true};
      } catch (error) {
        console.error('Code assistance error:', error);
        return {suggestions: [], incomplete: true};
      }
    },
  });
}

function monacoKind(kind: string): monaco.languages.CompletionItemKind {
  switch (kind.toLowerCase()) {
    case 'class':
      return monaco.languages.CompletionItemKind.Class;
    case 'method':
      return monaco.languages.CompletionItemKind.Method;
    case 'function':
      return monaco.languages.CompletionItemKind.Function;
    case 'variable':
      return monaco.languages.CompletionItemKind.Variable;
    case 'snippet':
      return monaco.languages.CompletionItemKind.Snippet;
    default:
      return monaco.languages.CompletionItemKind.Text;
  }
}

function monacoInsertTextRules(kind: string) {
  switch (kind.toLowerCase()) {
    case 'snippet':
      return monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet;
    default:
      return monaco.languages.CompletionItemInsertTextRule.None;
  }
}

function extractPath(lineContent: string, position: monaco.Position): string {
  const paths = []
  let match;
  // there are two groups: match[1] will be string withing double quotes, and match[2] means single quote string
  const regex = /"(\/[^"]*)"|'(\/[^']*)'/g;
  // retrieve all paths from current line
  while ((match = regex.exec(lineContent)) !== null) {
    if (match[1]) {
      paths.push({word: match[1], index: match['index'] + 1});
    } else if (match[2]) {
      paths.push({word: match[2], index: match['index'] + 1});
    }
  }
  // keep only matches before the cursor
  const possiblePaths = paths.filter(p => p.index < position.column)
  if (possiblePaths.length == 0) {
    return ''
  }
  // find the one closest to the cursor
  const result = possiblePaths
    .map(p => {
      return {diff: position.column - p.index, ...p}
    })
    .sort((a, b) => a.diff - b.diff)[0]

  return result.word || '';
}

function removePathPrefix(path: string, v: string) {
  if (path && v.startsWith(path)) {
    return v.substring(path.length);
  }
  return v;
}

/*
function sortText(label: string, word: string): string {
    const lastSegment = label.split('.').pop() || '';
    const isExactMatch = lastSegment === word;
    const isPartialMatch = lastSegment.includes(word);
    const score = (isExactMatch ? '0' : isPartialMatch ? '1' : '2');
    return score + "_" + label;
}
*/
