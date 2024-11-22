import { Monaco } from "@monaco-editor/react";
// import * as monaco from 'monaco-editor';

export function registerCompletions(instance: Monaco) {
    /*
    instance.languages.registerCompletionItemProvider('groovy', {
        provideCompletionItems: (model, position) => {
            const word = model.getWordUntilPosition(position);
            const range = {
                startLineNumber: position.lineNumber,
                endLineNumber: position.lineNumber,
                startColumn: word.startColumn,
                endColumn: word.endColumn
            };

            const suggestions = Object.keys(aemTypes.classes).map(className => {
                const shortName = className.split('.').pop() || className;
                return {
                    label: className,
                    kind: monaco.languages.CompletionItemKind.Class,
                    insertText: shortName,
                    detail: 'AEM/OSGi Class',
                    range: range,
                    command: {
                        id: 'addQualifiedImport',
                        title: 'Add Qualified Import',
                        arguments: [className]
                    }
                };
            });

            return { suggestions };
        }
    });
    */
}
