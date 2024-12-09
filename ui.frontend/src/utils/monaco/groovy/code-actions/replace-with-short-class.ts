import { Monaco } from "@monaco-editor/react";
import * as monaco from 'monaco-editor';
import { LANGUAGE_ID } from "../../groovy.ts";

export function replaceWithShortClass(instance: Monaco) {

    instance.languages.registerCodeActionProvider(LANGUAGE_ID, {
        provideCodeActions: (model: monaco.editor.ITextModel, range: monaco.Range) => {
            const codeActions = [] as monaco.languages.CodeAction[];

            // Get the content of the line where the code action is triggered.
            const lineContent = model.getLineContent(range.startLineNumber);

            // Regular expression to match fully qualified class names.
            const regex = /([a-zA-Z_][\w]*\.)+[A-Z][\w]*(?:\.[A-Z][\w]*)*/;
            const match = lineContent.match(regex);
            if (!match) {
                return { actions: codeActions, dispose: () => {} };
            }

            // Extract the fully qualified class name and the short class name.
            const fullyQualifiedClassName = match[0];
            const shortClassName = fullyQualifiedClassName.split('.').pop();

            // If the class name does not contain a dot, it is already a short class name.
            if (!fullyQualifiedClassName.includes('.')) {
                return { actions: codeActions, dispose: () => {} };
            }

            // Determine the range of the fully qualified class name in the line.
            const startColumn = lineContent.indexOf(fullyQualifiedClassName) + 1;
            const endColumn = startColumn + fullyQualifiedClassName.length;
            const matchRange = new monaco.Range(range.startLineNumber, startColumn, range.startLineNumber, endColumn);

            // Add a code action to replace the fully qualified class name with the short class name.
            codeActions.push({
                title: `Replace with short class name`,
                command: {
                    id: 'importClass',
                    title: 'Replace with short class name',
                    arguments: [fullyQualifiedClassName, shortClassName, matchRange]
                },
                kind: 'quickfix'
            });

            return { actions: codeActions, dispose: () => {} };
        }
    });

}
