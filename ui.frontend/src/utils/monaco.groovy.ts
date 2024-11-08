import { Monaco } from "@monaco-editor/react";
import { conf as javaLanguageConfiguration, language as javaLanguage } from 'monaco-editor/esm/vs/basic-languages/java/java.js';

export function registerGroovyLanguage(monaco: Monaco) {
    // Check if conf and language are not null or undefined
    if (!javaLanguageConfiguration || !javaLanguage) {
        console.error('Java language configuration or language is not available.');
        return;
    }

    // Clone the Java language configuration and tokenizer
    const groovyLanguageConfiguration = { ...javaLanguageConfiguration };
    const groovyLanguage = { ...javaLanguage };

    // Add Groovy-specific keywords
    groovyLanguage.keywords.push(
        "as", "def", "in", "trait", "with", "println", "print", "groovy"
    );

    // Add Groovy-specific operators
    groovyLanguage.operators.push(
        "?.", "?:", "*.", "->", "==~", "=~"
    );

    // Register the Groovy language using the modified configuration and tokenizer
    monaco.languages.register({ id: 'groovy' });
    monaco.languages.setMonarchTokensProvider('groovy', groovyLanguage);
    monaco.languages.setLanguageConfiguration('groovy', groovyLanguageConfiguration);
}
