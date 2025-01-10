import { Monaco } from '@monaco-editor/react';
import { registerAemCodeCompletions } from './completions/aem-code.ts';
import { registerSelfCodeCompletions } from './completions/self-code.ts';

export function registerCompletions(instance: Monaco) {
  registerSelfCodeCompletions(instance);
  registerAemCodeCompletions(instance);
}
