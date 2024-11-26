import * as monaco from 'monaco-editor';
import {Monaco} from "@monaco-editor/react";
import {LANGUAGE_ID} from "../../groovy.ts";
import {ApiDataAssistCode, apiRequest} from "../../../api.ts";

export function registerAemCodeCompletions(instance: Monaco) {
    instance.languages.registerCompletionItemProvider(LANGUAGE_ID, {

        provideCompletionItems: (model: monaco.editor.ITextModel, position: monaco.Position): monaco.languages.ProviderResult<monaco.languages.CompletionList> => {
            const word = model.getWordAtPosition(position)?.word || '';

            return apiRequest<ApiDataAssistCode>({
                method: "GET",
                url: `/apps/contentor/api/assist-code.json?word=${encodeURIComponent(word)}`,
                operation: "Code assistance"
            }).then(response => {
                const suggestions = response.data.data.suggestions.map(suggestion => ({
                    label: suggestion.value,
                    detail: suggestion.kind,
                    documentation: suggestion.doc,
                    kind: mapToMonacoKind(suggestion.kind),
                    insertText: suggestion.value,
                    range: new monaco.Range(
                        position.lineNumber,
                        model.getWordAtPosition(position)?.startColumn || position.column,
                        position.lineNumber,
                        model.getWordAtPosition(position)?.endColumn || position.column
                    )
                }));

                return { suggestions };
            }).catch(error => {
                console.error('Code assistance error:', error);
                return { suggestions: [] };
            });
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
