import { stripExtension } from './name';

export const SNIPPET_EXTENSION = '.yml';

/** Starter content of a snippet given its path under the snippets folder, e.g. `acme/hello`. */
export function snippetTemplate(relativePath: string): string {
  const parts = stripExtension(relativePath.trim(), SNIPPET_EXTENSION).split('/');
  const group = parts[0].charAt(0).toUpperCase() + parts[0].slice(1);
  return [
    `group: ${group}`,
    `name: ${parts.join('_')}`,
    'content: |',
    '  println "${1:message}"',
    'documentation: |',
    '  Describe what the snippet inserts and when to use it.',
    '',
  ].join('\n');
}
