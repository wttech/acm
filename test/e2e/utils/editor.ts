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

export async function readFromCodeEditor(page: Page, ariaLabel: string): Promise<string> {
  await page.waitForFunction((label: string) => {
    const editors = (window as any).monaco?.editor?.getEditors?.() || [];
    return editors.some((editor: any) => {
      const domNode = editor.getDomNode();
      const textarea = domNode?.querySelector('textarea');
      return textarea?.getAttribute('aria-label') === label;
    });
  }, ariaLabel, { timeout: 10000 });
  
  return await page.evaluate((label: string) => {
    const editors = (window as any).monaco?.editor?.getEditors?.() || [];
    const editor = editors.find((e: any) => {
      const domNode = e.getDomNode();
      const textarea = domNode?.querySelector('textarea');
      return textarea?.getAttribute('aria-label') === label;
    });
    const model = editor?.getModel();
    return model ? model.getValue() : '';
  }, ariaLabel);
}

export async function readFromCodeEditorAsJson<T = any>(
  page: Page, 
  ariaLabel: string
): Promise<T> {
  const content = await readFromCodeEditor(page, ariaLabel);
  return JSON.parse(content);
}
