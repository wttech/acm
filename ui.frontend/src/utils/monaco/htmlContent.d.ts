declare module 'monaco-editor/esm/vs/base/common/htmlContent' {
  export class MarkdownString {
    constructor(value?: string);
    value: string;
    isTrusted: boolean;
    supportThemeIcons: boolean;
    appendText(value: string): MarkdownString;
    appendMarkdown(value: string): MarkdownString;
    appendCodeblock(langId: string, code: string): MarkdownString;
  }
}
