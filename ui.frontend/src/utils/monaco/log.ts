import { Monaco } from '@monaco-editor/react';
import { DEFAULT_THEME_ID } from './theme.ts';

export const LOG_LANGUAGE_ID = 'acmLog';
export const LOG_THEME_ID = 'acmLog';

export function registerLogLanguage(instance: Monaco) {
  const already = instance.languages.getLanguages().some((l) => l.id === LOG_LANGUAGE_ID);
  if (already) {
    return;
  }

  instance.languages.register({ id: LOG_LANGUAGE_ID });

  instance.languages.setMonarchTokensProvider(LOG_LANGUAGE_ID, {
    tokenizer: {
      root: [
        [/^\[ERROR].*$/, 'log-error'],
        [/^\[WARN].*$/, 'log-warn'],
        [/^\[INFO].*$/, 'log-info'],
        [/^\[DEBUG].*$/, 'log-debug'],
        [/^\[TRACE].*$/, 'log-trace'],
      ],
    },
  });

  instance.editor.defineTheme(LOG_THEME_ID, {
    base: DEFAULT_THEME_ID,
    inherit: true,
    rules: [
      { token: 'log-error', foreground: 'f14c4c', fontStyle: 'bold' },
      { token: 'log-warn', foreground: 'e5c07b' },
      { token: 'log-info', foreground: 'ededed' },
      { token: 'log-debug', foreground: '8b949e' },
      { token: 'log-trace', foreground: '6a6a6a' },
    ],
    colors: {},
  });
}
