import { Monaco } from '@monaco-editor/react';

export const DEFAULT_THEME_ID = 'acm-dark';

export function registerTheme(instance: Monaco) {
  instance.editor.defineTheme(DEFAULT_THEME_ID, {
    base: 'vs-dark',
    inherit: true,
    rules: [],
    colors: {},
  });
}
