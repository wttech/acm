import { Monaco } from '@monaco-editor/react';

export const LOG_LANGUAGE_ID = 'acmLog';
export const LOG_THEME_ID = 'acmLog';

export function registerLogLanguage(instance: Monaco) {
  const already = instance.languages.getLanguages().some((l) => l.id === LOG_LANGUAGE_ID);
  if (already) {
    return;
  }

  instance.languages.register({ id: LOG_LANGUAGE_ID });

  // Monarch syntax (simple line-level classification based on leading [LEVEL])
  instance.languages.setMonarchTokensProvider(LOG_LANGUAGE_ID, {
    tokenizer: {
      root: [
        [/^\[ERROR].*$/, 'log-error'],
        [/^\[WARN].*$/, 'log-warn'],
        [/^\[INFO].*$/, 'log-info'],
        [/^\[DEBUG].*$/, 'log-debug'],
        [/^\[TRACE].*$/, 'log-trace'],
        // Optional timestamp + level: 2024-05-05 12:34:56,789 [INFO] ...
        [/^(?:\d{4}-\d{2}-\d{2}.*?\s)?\[ERROR].*$/, 'log-error'],
        [/^(?:\d{4}-\d{2}-\d{2}.*?\s)?\[WARN].*$/, 'log-warn'],
        [/^(?:\d{4}-\d{2}-\d{2}.*?\s)?\[INFO].*$/, 'log-info'],
        [/^(?:\d{4}-\d{2}-\d{2}.*?\s)?\[DEBUG].*$/, 'log-debug'],
        [/^(?:\d{4}-\d{2}-\d{2}.*?\s)?\[TRACE].*$/, 'log-trace'],
      ],
    },
  });

  // Theme (inherit dark base; adjust colors as needed)
  instance.editor.defineTheme(LOG_THEME_ID, {
    base: 'vs-dark',
    inherit: true,
    rules: [
      { token: 'log-error', foreground: 'ff6b68', fontStyle: 'bold' },
      { token: 'log-warn', foreground: 'ffd866' },
      { token: 'log-info', foreground: 'd4d4d4' },
      { token: 'log-debug', foreground: 'b0b0b0', fontStyle: 'italic' },
      { token: 'log-trace', foreground: '888888', fontStyle: 'italic' },
    ],
    colors: {},
  });
}
