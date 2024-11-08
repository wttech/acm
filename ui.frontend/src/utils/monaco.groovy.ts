import { Monaco } from "@monaco-editor/react";
import * as monaco from 'monaco-editor';
import { conf as javaLanguageConfiguration, language as javaLanguage } from 'monaco-editor/esm/vs/basic-languages/java/java.js';
import * as osgi from './osgi';

export function registerGroovyLanguage(instance: Monaco) {
    registerSyntax(instance);
    registerCommands(instance);
    registerCompletions(instance);
    registerCodeActions(instance);
}

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
            [/"([^"\\]|\\.)*$/, 'string.invalid'],  // non-terminated string
            [/"/, { token: 'string.quote', bracket: '@open', next: '@gstring' }],

            // Groovy closures
            [/\{/, { token: 'delimiter.curly', next: '@closure' }]
        ],

        gstring: [
            [/\$\{[^}]+\}/, 'variable'],
            [/[^\\"]+/, 'string'],
            [/\\./, 'string.escape'],
            [/"/, { token: 'string.quote', bracket: '@close', next: '@pop' }]
        ],

        closure: [
            [/[^\{\}]+/, ''],
            [/\{/, 'delimiter.curly', '@push'],
            [/\}/, 'delimiter.curly', '@pop']
        ]
    };

    // Register the Groovy language using the modified configuration and tokenizer
    instance.languages.register({ id: 'groovy' });
    instance.languages.setMonarchTokensProvider('groovy', groovyLanguage);
    instance.languages.setLanguageConfiguration('groovy', groovyLanguageConfiguration);
}

function registerCommands(instance: Monaco) {
    instance.editor.registerCommand('addQualifiedImport', (_, className) => {
        const model = instance.editor.getModels()[0];
        const fullText = model.getValue();
        const importStatement = `import ${className};\n`;

        if (!fullText.includes(importStatement)) {
            model.pushEditOperations([], [{
                range: new monaco.Range(1, 1, 1, 1),
                text: importStatement
            }], () => null);
        }
    });
}

function registerCompletions(instance: Monaco) {
    instance.languages.registerCompletionItemProvider('groovy', {
        provideCompletionItems: (model, position) => {
            const word = model.getWordUntilPosition(position);
            const range = {
                startLineNumber: position.lineNumber,
                endLineNumber: position.lineNumber,
                startColumn: word.startColumn,
                endColumn: word.endColumn
            };

            const suggestions = osgi.classNames.map(className => {
                const shortName = className.split('.').pop() || className;
                return {
                    label: className,
                    kind: monaco.languages.CompletionItemKind.Class,
                    insertText: shortName,
                    detail: 'AEM/OSGi Class',
                    range: range,
                    command: {
                        id: 'addQualifiedImport',
                        title: 'Add Qualified Import',
                        arguments: [className]
                    }
                };
            });

            return { suggestions };
        }
    });
}

function registerCodeActions(instance: Monaco) {
    instance.languages.registerCodeActionProvider('groovy', {
        provideCodeActions: (model: monaco.editor.ITextModel, range: monaco.Range) => {
            const codeActions = [] as monaco.languages.CodeAction[];

            /*
            codeActions.push({
                title: `Shorten Fully Qualified Class Name ${fullyQualifiedClassName}`,
                command: {
                    id: 'shortenClassName',
                    title: 'Shorten Class Name',
                    arguments: [fullyQualifiedClassName, matchRange]
                },
                kind: 'quickfix'
            });
            */

            return { actions: codeActions, dispose: () => {} };
        }
    });
}
