import { Monaco } from '@monaco-editor/react';
import { registerCodeActions } from './groovy/code-actions.ts';
import { registerCommands } from './groovy/commands.ts';
import { registerCompletions } from './groovy/completions.ts';
import { registerSyntax } from './groovy/syntax.ts';

export const GROOVY_LANGUAGE_ID = 'groovy';

export function registerGroovyLanguage(instance: Monaco) {
  const languages = instance.languages.getLanguages();
  const registered = languages.some((lang) => lang.id === GROOVY_LANGUAGE_ID);

  if (!registered) {
    registerSyntax(instance);
    registerCommands(instance);
    registerCompletions(instance);
    registerCodeActions(instance);
  }
}
