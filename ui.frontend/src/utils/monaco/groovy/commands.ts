import { Monaco } from "@monaco-editor/react";
import * as monaco from 'monaco-editor';

export function registerCommands(instance: Monaco) {
    instance.editor.registerCommand('addQualifiedImport', (_, fullyQualifiedClassName) => {
        const model = instance.editor.getModels()[0];

        // TODO ...
    });
}
