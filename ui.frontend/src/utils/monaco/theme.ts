import { Monaco } from '@monaco-editor/react';

export const BASE_THEME_ID = 'vs-dark';
export const DEFAULT_THEME_ID = 'acm-dark';

export function registerTheme(instance: Monaco) {
  instance.editor.defineTheme(DEFAULT_THEME_ID, {
    base: BASE_THEME_ID,
    inherit: true,
    rules: [],
    colors: {},
  });
}
