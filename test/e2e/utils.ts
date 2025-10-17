export class Strings {
  static dedent(text: string): string {
    const lines = text.split('\n');
    // Find minimum indentation (ignoring empty lines)
    const minIndent = lines
      .filter((line) => line.trim().length > 0)
      .reduce((min, line) => {
        const indent = line.match(/^(\s*)/)?.[1].length || 0;
        return Math.min(min, indent);
      }, Infinity);

    // Remove common leading whitespace
    return lines.map((line) => line.slice(minIndent === Infinity ? 0 : minIndent)).join('\n');
  }
}

export async function writeToCodeEditor(page: any, editorIndex: number, code: string) {
  const cleanCode = Strings.dedent(code);
  
  await page.waitForFunction((data: { index: number }) => {
    const editors = (window as any).monaco?.editor?.getEditors?.() || [];
    return editors.length > data.index;
  }, { index: editorIndex }, { timeout: 10000 });
  
  await page.evaluate((data: { index: number, text: string }) => {
    const editors = (window as any).monaco?.editor?.getEditors?.() || [];
    
    if (data.index >= editors.length) {
      throw new Error(`Code editor index ${data.index} not found. Available editors: ${editors.length}`);
    }
    
    const editor = editors[data.index];
    editor.setValue(data.text);
  }, { index: editorIndex, text: cleanCode });

  await page.waitForTimeout(500);
}

export async function readFromCodeEditor(page: any, editorIndex: number): Promise<string> {
  await page.waitForFunction((index: number) => {
    const editors = (window as any).monaco?.editor?.getEditors?.() || [];
    return editors.length > index;
  }, editorIndex, { timeout: 10000 });
  
  return await page.evaluate((index: number) => {
    const editors = (window as any).monaco?.editor?.getEditors?.() || [];
    
    if (index >= editors.length) {
      throw new Error(`Code editor index ${index} not found. Available editors: ${editors.length}`);
    }
    
    const editor = editors[index];
    const model = editor.getModel();
    return model ? model.getValue() : '';
  }, editorIndex);
}