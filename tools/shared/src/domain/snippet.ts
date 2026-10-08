import { stripExtension } from './name';

export const SNIPPET_EXTENSION = '.yml';

/** What a user sees of snippets: their name and what they are for. */
export const SNIPPET_INFO = {
  label: 'Snippets',
  description: 'Code templates offered in the ACM Console and on the Snippets page, to insert into scripts.',
};

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
