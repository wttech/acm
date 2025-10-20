import { Strings } from "./lang";

export async function writeToCodeEditor(page: any, code: string, editorIndex: number = 0) {
  const codeClean = Strings.dedent(code);
  
  await page.waitForFunction((data: { index: number }) => {
    const editors = (window as any).monaco?.editor?.getEditors?.() || [];
    if (editors.length <= data.index) {
      return false;
    }
    
    const editor = editors[data.index];
    return editor && editor.getModel();
  }, { index: editorIndex }, { timeout: 10000 });
  
  await page.evaluate((data: { index: number, text: string }) => {
    const editors = (window as any).monaco?.editor?.getEditors?.() || [];
    const editor = editors[data.index];
    editor.setValue(data.text);
    editor.layout();
  }, { index: editorIndex, text: codeClean });
}

export async function readFromCodeEditor(page: any, editorIndex: number = 0): Promise<string> {
  await page.waitForFunction((index: number) => {
    const editors = (window as any).monaco?.editor?.getEditors?.() || [];
    if (editors.length <= index) {
      return false;
    }
    
    const editor = editors[index];
    const model = editor.getModel();
    const value = model ? model.getValue() : '';
    
    return value.trim().length > 0;
  }, editorIndex, { timeout: 10000 });
  
  return await page.evaluate((index: number) => {
    const editors = (window as any).monaco?.editor?.getEditors?.() || [];
    const editor = editors[index];
    const model = editor.getModel();
    return model ? model.getValue() : '';
  }, editorIndex);
}