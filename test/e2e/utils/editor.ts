import { Page } from "@playwright/test";
import { Strings } from "./lang";

export async function writeToCodeEditor(page: Page, code: string, editorIndex: number = 0) {
  const codeClean = Strings.dedent(code).trim();
  
  await page.waitForFunction((data: { editorIndex: number }) => {
    const editors = (window as any).monaco?.editor?.getEditors?.() || [];
    if (editors.length <= data.editorIndex) {
      return false;
    }
    
    const editor = editors[data.editorIndex];
    return editor && editor.getModel();
  }, { editorIndex }, { timeout: 10000 });
  
  await page.evaluate((data: { editorIndex: number, codeClean: string }) => {
    const editors = (window as any).monaco?.editor?.getEditors?.() || [];
  const editor = editors[data.editorIndex];
    editor.setValue(data.codeClean);
    editor.layout();
  }, { editorIndex, codeClean });
}

export async function readFromCodeEditor(page: Page, editorIndex: number = 0): Promise<string> {
  await page.waitForFunction((data: { editorIndex: number }) => {
    const editors = (window as any).monaco?.editor?.getEditors?.() || [];
    if (editors.length <= data.editorIndex) {
      return false;
    }
    
    const editor = editors[data.editorIndex];
    const model = editor.getModel();
    const value = model ? model.getValue() : '';
    
    return value.trim().length > 0;
  }, { editorIndex }, { timeout: 10000 });
  
  return await page.evaluate((data: { editorIndex: number }) => {
    const editors = (window as any).monaco?.editor?.getEditors?.() || [];
    const editor = editors[data.editorIndex];
    const model = editor.getModel();
    return model ? model.getValue() : '';
  }, { editorIndex });
}
