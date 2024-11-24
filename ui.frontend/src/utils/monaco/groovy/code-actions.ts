import { Monaco } from "@monaco-editor/react";
import * as monaco from 'monaco-editor';
import { registerCommands } from './commands';

export function registerCodeActions(instance: Monaco) {
    registerCommands(instance);

    instance.languages.registerCodeActionProvider('groovy', {
        provideCodeActions: (model: monaco.editor.ITextModel, range: monaco.Range) => {
            const codeActions = [] as monaco.languages.CodeAction[];

            // TODO ...

            return { actions: codeActions, dispose: () => {} };
        }
    });
}
