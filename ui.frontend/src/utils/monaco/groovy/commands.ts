import { Monaco } from "@monaco-editor/react";

export function registerCommands(instance: Monaco) {
    instance.editor.registerCommand('importClass', (accessor, ...args) => {
        const [fullyQualifiedClassName] = args;
        const model = instance.editor.getModels()[0];

        // Add the import statement at the beginning of the file
        const importStatement = `import ${fullyQualifiedClassName};\n`;
        const fullText = model.getValue();
        const newText = importStatement + fullText;
        model.setValue(newText);

        // Find all instances of the fully qualified class name and replace them with the short class name
        const shortClassName = fullyQualifiedClassName.split('.').pop();
        const regex = new RegExp(`\\b${fullyQualifiedClassName}\\b`, 'g');
        const lines = newText.split('\n');
        const updatedLines = lines.map(line => {
            if (line.startsWith('import ')) {
                return line;
            }
            return line.replace(regex, shortClassName);
        });
        const updatedText = updatedLines.join('\n');

        model.setValue(updatedText);
    });
}
