import { Monaco } from '@monaco-editor/react';
import { registerAemCodeCompletions } from './completions/aem-code.ts';
import { registerCoreCompletions } from './completions/core.ts';
import { registerSelfCodeCompletions } from './completions/self-code.ts';

export function registerCompletions(instance: Monaco) {
  registerCoreCompletions(instance);
  registerSelfCodeCompletions(instance);
  registerAemCodeCompletions(instance);
}
