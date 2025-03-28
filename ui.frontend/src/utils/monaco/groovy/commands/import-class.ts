import { Monaco } from '@monaco-editor/react';

export function registerImportClass(instance: Monaco) {
  instance.editor.registerCommand('importClass', (accessor, ...args) => {
    const [fullyQualifiedClassName] = args;

    const model = instance.editor.getModels()[0];

    const shortClassName = fullyQualifiedClassName.split('.').pop();
    const fullText = model.getValue();
    const importStatement = `import ${fullyQualifiedClassName};\n`;

    // Check if the import statement already exists
    if (!fullText.includes(importStatement)) {
      // Add the import statement at the beginning of the file
      model.pushEditOperations(
        [],
        [
          {
            range: new instance.Range(1, 1, 1, 1),
            text: importStatement,
          },
        ],
        () => null,
      );
    }

    // Find all instances of the fully qualified class name and replace them with the short class name
    const regex = new RegExp(`\\b${fullyQualifiedClassName}\\b`, 'g');
    const lines = model.getValue().split('\n');
    const updatedLines = lines.map((line) => {
      if (line.startsWith('import ')) {
        return line;
      }
      return line.replace(regex, shortClassName);
    });

    // Sort import statements alphabetically
    const importLines = updatedLines.filter((line) => line.startsWith('import ')).sort();
    const nonImportLines = updatedLines.filter((line) => !line.startsWith('import '));

    // Ensure there is a single blank line between imports and the rest of the code
    if (importLines.length > 0 && nonImportLines.length > 0 && nonImportLines[0].trim() !== '') {
      importLines.push('');
    }

    const updatedText = [...importLines, ...nonImportLines].join('\n');

    model.pushEditOperations(
      [],
      [
        {
          range: model.getFullModelRange(),
          text: updatedText,
        },
      ],
      () => null,
    );
  });
}
