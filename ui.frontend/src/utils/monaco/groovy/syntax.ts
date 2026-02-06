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

  // Copy Java root rules as base
  const javaRootRules = [...(groovyLanguage.tokenizer.root || [])];

  // Extend the tokenizer with Groovy-specific features
  groovyLanguage.tokenizer = {
    ...groovyLanguage.tokenizer,
    root: [
      // Groovy-specific rules MUST come before Java rules

      // Import statements - color the whole qualified name
      [/(import)(\s+)([\w.]+)/, ['keyword', 'white', 'type.identifier']],

      // Slashy strings (regex): /pattern/
      // Only match when followed by regex-indicating chars, not division or comments
      [/\/(?=[[(^.\\a-zA-Z])/, { token: 'regexp', next: '@slashy_string' }],

      // Triple-quoted strings (must be before double quote)
      [/"""/, { token: 'string.quote', bracket: '@open', next: '@string_multiline' }],

      // Double-quoted strings with GString interpolation
      [/"/, { token: 'string.quote', bracket: '@open', next: '@string_double' }],

      // Single-quoted strings (Groovy treats these as strings, not char literals)
      [/'/, { token: 'string.quote', bracket: '@open', next: '@string_single' }],

      // Constants (UPPER_SNAKE_CASE) - before types to take precedence
      [/[A-Z][A-Z0-9_]+\b/, 'constant'],

      // Type names (PascalCase identifiers)
      [/[A-Z][\w$]*/, 'type.identifier'],

      // Java rules come after
      ...javaRootRules,
    ],

    // Slashy string state (regex literal)
    slashy_string: [
      [/\\./, 'regexp.escape'], // Escaped chars (including \/)
      [/\//, { token: 'regexp', next: '@pop' }], // Closing /
      [/[^\\/\r\n]+/, 'regexp'], // Content
      [/\r?\n/, { token: '', next: '@pop' }], // Newline = exit (error recovery)
    ],

    // Double-quoted string with GString interpolation
    string_double: [
      [/\\\$/, 'string.escape'], // Escaped $
      [/\$\{/, { token: 'identifier', bracket: '@open', next: '@gstring_expression' }], // ${...}
      [/\\./, 'string.escape'], // Escape sequences
      [/[^\\"$]+/, 'string'], // Regular content
      [/"/, { token: 'string.quote', bracket: '@close', next: '@pop' }], // Closing "
      [/[$]/, 'string'], // Lone $ at end
    ],

    // Single-quoted string (no interpolation)
    string_single: [
      [/[^\\']+/, 'string'], // Regular content
      [/\\./, 'string.escape'], // Escape sequences
      [/'/, { token: 'string.quote', bracket: '@close', next: '@pop' }], // Closing '
    ],

    // Triple-quoted multiline string with GString interpolation
    string_multiline: [
      [/\\\$/, 'string.escape'], // Escaped $
      [/\$\{/, { token: 'identifier', bracket: '@open', next: '@gstring_expression_multiline' }], // ${...}
      [/\\./, 'string.escape'], // Escape sequences
      [/[^\\"$]+/, 'string'], // Regular content
      [/"""/, { token: 'string.quote', bracket: '@close', next: '@pop' }], // Closing """
      [/"/, 'string'], // Single " inside multiline
      [/[$]/, 'string'], // Lone $
    ],

    // GString expression ${...}
    gstring_expression: [
      [/'/, { token: 'string.quote', bracket: '@open', next: '@string_in_gstring_single' }],
      [/\{/, { token: 'delimiter.curly', bracket: '@open', next: '@closure' }],
      [/\}/, { token: 'identifier', bracket: '@close', next: '@pop' }],
      [/[^{}'"]+/, 'identifier'],
    ],

    // GString expression for multiline strings
    gstring_expression_multiline: [
      [/'/, { token: 'string.quote', bracket: '@open', next: '@string_in_gstring_single' }],
      [/"/, { token: 'string.quote', bracket: '@open', next: '@string_in_gstring_double' }],
      [/\{/, { token: 'delimiter.curly', bracket: '@open', next: '@closure' }],
      [/\}/, { token: 'identifier', bracket: '@close', next: '@pop' }],
      [/[^{}'"]+/, 'identifier'],
    ],

    // Single-quoted string inside GString expression
    string_in_gstring_single: [
      [/[^\\']+/, 'string'],
      [/\\./, 'string.escape'],
      [/'/, { token: 'string.quote', bracket: '@close', next: '@pop' }],
    ],

    // Double-quoted string inside GString expression
    string_in_gstring_double: [
      [/[^\\"]+/, 'string'],
      [/\\./, 'string.escape'],
      [/"/, { token: 'string.quote', bracket: '@close', next: '@pop' }],
    ],

    // Groovy closure { ... } - simple version, relies on root rules for nested content
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
