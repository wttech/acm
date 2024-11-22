import { Monaco } from "@monaco-editor/react";
import * as monaco from 'monaco-editor';

export function registerCodeActions(instance: Monaco) {
    instance.languages.registerCodeActionProvider('groovy', {
        provideCodeActions: (model: monaco.editor.ITextModel, range: monaco.Range) => {
            const codeActions = [] as monaco.languages.CodeAction[];

            /*
            codeActions.push({
                title: `Shorten Fully Qualified Class Name ${fullyQualifiedClassName}`,
                command: {
                    id: 'shortenClassName',
                    title: 'Shorten Class Name',
                    arguments: [fullyQualifiedClassName, matchRange]
                },
                kind: 'quickfix'
            });
            */

            return { actions: codeActions, dispose: () => {} };
        }
    });
}
