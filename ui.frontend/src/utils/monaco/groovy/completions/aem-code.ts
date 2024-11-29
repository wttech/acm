import * as monaco from 'monaco-editor';
import {Monaco} from "@monaco-editor/react";
import {LANGUAGE_ID} from "../../groovy.ts";
import {ApiDataAssistCode, apiRequest} from "../../../api.ts";

export function registerAemCodeCompletions(instance: Monaco) {
    instance.languages.registerCompletionItemProvider(LANGUAGE_ID, {
        provideCompletionItems: async (model: monaco.editor.ITextModel, position: monaco.Position): Promise<monaco.languages.CompletionList> => {
            const wordAtPosition = model.getWordAtPosition(position);
            const wordText = wordAtPosition?.word || '';

            console.info('Code assistance word:', wordText);

            try {
                const response = await apiRequest<ApiDataAssistCode>({
                    method: "GET",
                    url: `/apps/contentor/api/assist-code.json?word=${encodeURIComponent(wordText)}`,
                    operation: "Code assistance"
                });
                const assistance = response.data.data;

                // TODO support completing when cursor is in the middle of a word

                const filteredSuggestions = (assistance?.suggestions ?? []).map(suggestion => ({
                    label: suggestion.v,
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

                return { suggestions: filteredSuggestions, incomplete: true };
            } catch (error) {
                console.error('Code assistance error:', error);
                return { suggestions: [], incomplete: true };
            }
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
