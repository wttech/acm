import { Monaco } from '@monaco-editor/react';
import { language as javaLanguage, conf as javaLanguageConfiguration } from 'monaco-editor/esm/vs/basic-languages/java/java.js';
import { LANGUAGE_ID } from '../groovy.ts';

export function registerSyntax(instance: Monaco) {
  // Check if conf and language are not null or undefined
  if (!javaLanguageConfiguration || !javaLanguage) {
    console.error('Java language configuration or language is not available.');
    return;
  }

  // Clone the Java language configuration and tokenizer
  const groovyLanguageConfiguration = { ...javaLanguageConfiguration };
  const groovyLanguage = { ...javaLanguage };

  // Add Groovy-specific keywords
  const groovyKeywords = ['def', 'as', 'in', 'trait', 'with'];
  groovyLanguage.keywords = [...(groovyLanguage.keywords || []), ...groovyKeywords];

  // Extend the tokenizer with Groovy-specific features
  groovyLanguage.tokenizer = {
    ...groovyLanguage.tokenizer,
    root: [
      ...(groovyLanguage.tokenizer.root || []),

      // Support for GStrings (interpolated strings)
      [/"([^"\\]|\\.)*$/, 'string.invalid'], // non-terminated string
      [/"/, { token: 'string.quote', bracket: '@open', next: '@gstring' }],

      // Groovy closures
      [/\{/, { token: 'delimiter.curly', next: '@closure' }],
    ],

    gstring: [
      [/\$\{[^}]+\}/, 'variable'],
      [/[^\\"]+/, 'string'],
      [/\\./, 'string.escape'],
      [/"/, { token: 'string.quote', bracket: '@close', next: '@pop' }],
    ],

    closure: [
      [/[^\{\}]+/, ''],
      [/\{/, 'delimiter.curly', '@push'],
      [/\}/, 'delimiter.curly', '@pop'],
    ],
  };

  // Register the Groovy language using the modified configuration and tokenizer
  instance.languages.register({ id: LANGUAGE_ID });
  instance.languages.setMonarchTokensProvider(LANGUAGE_ID, groovyLanguage);
  instance.languages.setLanguageConfiguration(LANGUAGE_ID, groovyLanguageConfiguration);
}
