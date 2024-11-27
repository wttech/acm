import * as monaco from 'monaco-editor';
import {Monaco} from "@monaco-editor/react";
import {LANGUAGE_ID} from "../../groovy.ts";
import {ApiDataAssistCode, apiRequest} from "../../../api.ts";

let aemDataAssistCode: ApiDataAssistCode | null = null;

export function registerAemCodeCompletions(instance: Monaco) {
    // Fetch all completions once during initialization
    apiRequest<ApiDataAssistCode>({
        method: "GET",
        url: `/apps/contentor/api/assist-code.json`,
        operation: "Code assistance"
    }).then(response => {
        aemDataAssistCode = response.data.data
    }).catch(error => {
        console.error('Code assistance error:', error);
    });

    instance.languages.registerCompletionItemProvider(LANGUAGE_ID, {
        provideCompletionItems: (model: monaco.editor.ITextModel, position: monaco.Position): monaco.languages.ProviderResult<monaco.languages.CompletionList> => {
            const wordAtPosition = model.getWordAtPosition(position);
            const wordText = wordAtPosition?.word || '';

            console.info('Code assistance word:',  wordText);

            const filteredSuggestions = (aemDataAssistCode?.suggestions ?? []).map(suggestion => ({
                label: suggestion.v, // TODO troubleshoot with: sortText(suggestion.v, wordText)
                insertText: suggestion.v,
                kind: mapToMonacoKind(suggestion.k),
                detail: suggestion.k,
                documentation: suggestion.i,
                range: new monaco.Range(
                    position.lineNumber,
                    wordAtPosition?.startColumn || position.column,
                    position.lineNumber,
                    wordAtPosition?.endColumn || position.column
                ),
                // TODO: sortText: sortText(suggestion.v, wordText)
            }));

            // TODO support completing when cursor is in the middle of a word

            return { suggestions: filteredSuggestions, incomplete: true };
        }
    });
}

function mapToMonacoKind(kind: string): monaco.languages.CompletionItemKind {
    switch (kind.toLowerCase()) {
        case 'class':
            return monaco.languages.CompletionItemKind.Class;
        case 'method':
            return monaco.languages.CompletionItemKind.Method;
        case 'function':
            return monaco.languages.CompletionItemKind.Function;
        case 'variable':
            return monaco.languages.CompletionItemKind.Variable;
        default:
            return monaco.languages.CompletionItemKind.Text;
    }
}

function sortText(label: string, word: string): string {
    const lastSegment = label.split('.').pop() || '';
    const isExactMatch = lastSegment === word;
    const isPartialMatch = lastSegment.includes(word);
    const score = (isExactMatch ? '0' : isPartialMatch ? '1' : '2');
    return score + "_" + label;
}

