import type { CatalogEntry } from './lifecycle';

// Mirrors core/.../code/Outputs.java.
export const OUTPUT_METHODS: CatalogEntry[] = [
  {
    name: 'file',
    signature: 'FileOutput outputs.file(String name, Closure options = null)',
    docs: 'Declares a downloadable file output.',
    snippet: "file('${1:name}') {\n\t$0\n}",
  },
  {
    name: 'text',
    signature: 'TextOutput outputs.text(String name, Closure options = null)',
    docs: 'Declares a text output.',
    snippet: "text('${1:name}') {\n\t$0\n}",
  },
];
