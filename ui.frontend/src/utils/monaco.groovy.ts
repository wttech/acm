import {Monaco} from "@monaco-editor/react";

export function registerGroovyLanguage(monaco: Monaco) {
    monaco.languages.register({ id: 'groovy' });
    monaco.languages.setMonarchTokensProvider('groovy', {
        tokenizer: {
            root: [
                [/\b(def|class|if|else|for|while|return)\b/, 'keyword'],
                [/[a-z_$][\w$]*/, 'identifier'],
                [/\d+/, 'number'],
                [/".*?"/, 'string'],
                [/\/\/.*$/, 'comment'],
            ]
        }
    });
}
