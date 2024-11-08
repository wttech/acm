import { Monaco } from "@monaco-editor/react";
import { conf as javaLanguageConfiguration, language as javaLanguage } from 'monaco-editor/esm/vs/basic-languages/java/java.js';

export function registerGroovyLanguage(monaco: Monaco) {
    // Check if conf and language are not null or undefined
    if (!javaLanguageConfiguration || !javaLanguage) {
        console.error('Java language configuration or language is not available.');
        return;
    }

    // Register the Groovy language using Java's configuration and tokenizer
    monaco.languages.register({ id: 'groovy' });
    monaco.languages.setMonarchTokensProvider('groovy', javaLanguage);
    monaco.languages.setLanguageConfiguration('groovy', javaLanguageConfiguration);
}
