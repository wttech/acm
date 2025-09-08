import { Monaco } from '@monaco-editor/react';
import { DEFAULT_THEME_ID } from './theme';

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
        [/^(\[\d{2}:\d{2}:\d{2}\.\d{3}\])?\[ERROR\].*$/, 'log-error'],
        [/^(\[\d{2}:\d{2}:\d{2}\.\d{3}\])?\[WARN\].*$/, 'log-warn'],
        [/^(\[\d{2}:\d{2}:\d{2}\.\d{3}\])?\[INFO\].*$/, 'log-info'],
        [/^(\[\d{2}:\d{2}:\d{2}\.\d{3}\])?\[DEBUG\].*$/, 'log-debug'],
        [/^(\[\d{2}:\d{2}:\d{2}\.\d{3}\])?\[TRACE\].*$/, 'log-trace'],
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
