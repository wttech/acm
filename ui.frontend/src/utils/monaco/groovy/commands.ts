import { Monaco } from "@monaco-editor/react";
import * as monaco from 'monaco-editor';

export function registerCommands(instance: Monaco) {
    instance.editor.registerCommand('addQualifiedImport', (_, className) => {
        const model = instance.editor.getModels()[0];
        const fullText = model.getValue();
        const importStatement = `import ${className};\n`;

        if (!fullText.includes(importStatement)) {
            model.pushEditOperations([], [{
                range: new monaco.Range(1, 1, 1, 1),
                text: importStatement
            }], () => null);
        }
    });
}
