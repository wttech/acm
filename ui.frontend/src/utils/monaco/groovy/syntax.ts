import { Monaco } from '@monaco-editor/react';
import { language as javaLanguage, conf as javaLanguageConfiguration } from 'monaco-editor/esm/vs/basic-languages/java/java.js';
import { GROOVY_LANGUAGE_ID } from '../groovy.ts';

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

  const javaRootRules = [...(groovyLanguage.tokenizer.root || [])].filter((rule) => {
    // Removes Java's single quote interpretation from tokenizer
    if (Array.isArray(rule) && rule[0] instanceof RegExp && typeof rule[1] === 'string') {
      return !rule[1].includes('string');
    }
    return true;
  });

  // Extend the tokenizer with Groovy-specific features
  groovyLanguage.tokenizer = {
    ...groovyLanguage.tokenizer,
    root: [
      ...javaRootRules,

      // multiline strings
      [/"""/, { token: 'string.quote', bracket: '@open', next: '@string_multiline' }],

      // double quoted strings
      [/"/, { token: 'string.quote', bracket: '@open', next: '@string_double' }],

      // single quoted strings
      [/'/, { token: 'string.quote', bracket: '@open', next: '@string_single' }],

      // Groovy closures
      [/\{/, { token: 'delimiter.curly', next: '@closure' }],
    ],

    string_double: [
      [/\\\$/, 'string.escape'],
      [/\$\{/, { token: 'identifier', bracket: '@open', next: '@gstring_expression' }],
      [/\\./, 'string.escape'],
      [/[^\\"$]+/, 'string'],
      [/"/, { token: 'string.quote', bracket: '@close', next: '@pop' }],
      [/[$]/, 'string'],
    ],

    string_single: [
      [/[^\\']+/, 'string'],
      [/\\./, 'string.escape'],
      [/'/, { token: 'string.quote', bracket: '@close', next: '@pop' }],
    ],

    string_multiline: [
      [/\\\$/, 'string.escape'],
      [/\$\{/, { token: 'identifier', bracket: '@open', next: '@gstring_expression_multiline' }],
      [/\\./, 'string.escape'],
      [/[^\\"$]+/, 'string'],
      [/"""/, { token: 'string.quote', bracket: '@close', next: '@pop' }],
      [/"/, 'string'],
      [/[$]/, 'string'],
    ],

    gstring_expression: [
      [/'/, { token: 'string.quote', bracket: '@open', next: '@string_in_gstring_single' }],
      [/\{/, { token: 'delimiter.curly', bracket: '@open', next: '@closure' }],
      [/\}/, { token: 'identifier', bracket: '@close', next: '@pop' }],
      [/[^{}'"]+/, 'identifier'],
    ],

    gstring_expression_multiline: [
      [/'/, { token: 'string.quote', bracket: '@open', next: '@string_in_gstring_single' }],
      [/"/, { token: 'string.quote', bracket: '@open', next: '@string_in_gstring_double' }],
      [/\{/, { token: 'delimiter.curly', bracket: '@open', next: '@closure' }],
      [/\}/, { token: 'identifier', bracket: '@close', next: '@pop' }],
      [/[^{}'"]+/, 'identifier'],
    ],

    string_in_gstring_single: [
      [/[^\\']+/, 'string'],
      [/\\./, 'string.escape'],
      [/'/, { token: 'string.quote', bracket: '@close', next: '@pop' }],
    ],

    string_in_gstring_double: [
      [/[^\\"]+/, 'string'],
      [/\\./, 'string.escape'],
      [/"/, { token: 'string.quote', bracket: '@close', next: '@pop' }],
    ],

    closure: [
      [/[^{}]+/, ''],
      [/\{/, 'delimiter.curly', '@push'],
      [/\}/, 'delimiter.curly', '@pop'],
    ],
  };

  // Register the Groovy language using the modified configuration and tokenizer
  instance.languages.register({ id: GROOVY_LANGUAGE_ID });
  instance.languages.setMonarchTokensProvider(GROOVY_LANGUAGE_ID, groovyLanguage);
  instance.languages.setLanguageConfiguration(GROOVY_LANGUAGE_ID, groovyLanguageConfiguration);
}
