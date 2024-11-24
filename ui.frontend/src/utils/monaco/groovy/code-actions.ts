import { Monaco } from "@monaco-editor/react";
import * as monaco from 'monaco-editor';

export function registerCodeActions(instance: Monaco) {
    instance.languages.registerCodeActionProvider('groovy', {
        provideCodeActions: (model: monaco.editor.ITextModel, range: monaco.Range) => {
            const codeActions = [] as monaco.languages.CodeAction[];
            const lineContent = model.getLineContent(range.startLineNumber);
            const cursorPosition = range.startColumn - 1;
            const textBeforeCursor = lineContent.substring(0, cursorPosition).trimEnd();
            const regex = /(?:[a-zA-Z_][\w]*\.)*[a-zA-Z_][\w]*$/;
            const match = textBeforeCursor.match(regex);

            if (!match) {
                return { actions: codeActions, dispose: () => {} };
            }

            const fullyQualifiedClassName = match[0];
            const shortClassName = fullyQualifiedClassName.split('.').pop();
            const startColumn = textBeforeCursor.lastIndexOf(fullyQualifiedClassName) + 1;
            const endColumn = startColumn + fullyQualifiedClassName.length;
            const matchRange = new monaco.Range(range.startLineNumber, startColumn, range.startLineNumber, endColumn);

            codeActions.push({
                title: `Import Class Name ${fullyQualifiedClassName}`,
                command: {
                    id: 'importClass',
                    title: 'Import Class Name',
                    arguments: [fullyQualifiedClassName, matchRange, shortClassName]
                },
                kind: 'quickfix'
            });

            return { actions: codeActions, dispose: () => {} };
        }
    });
}
