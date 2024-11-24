import * as monaco from 'monaco-editor';
import * as ts from 'typescript';
import {Monaco} from "@monaco-editor/react";

// Function to parse the TypeScript declaration file and extract symbols (including methods and classes)
function parseDTSFile(fileContent: string) {
    const sourceFile = ts.createSourceFile('aem.d.ts', fileContent, ts.ScriptTarget.Latest, true);

    const suggestions: monaco.languages.CompletionItem[] = [];

    // Helper function to get the fully qualified name of a node
    function getFullyQualifiedName(node: ts.Node): string {
        const parts: string[] = [];
        let current: ts.Node | undefined = node;
        while (current) {
            if (ts.isModuleDeclaration(current) || ts.isClassDeclaration(current) || ts.isInterfaceDeclaration(current)) {
                parts.unshift(current.name.text);
            }
            current = current.parent;
        }
        return parts.join('.');
    }

    // Recursive function to traverse the AST
    function visitNode(node: ts.Node) {
        // Interface declarations
        if (ts.isInterfaceDeclaration(node)) {
            const fullyQualifiedName = getFullyQualifiedName(node);
            suggestions.push({
                label: fullyQualifiedName,
                kind: monaco.languages.CompletionItemKind.Interface,
                insertText: fullyQualifiedName,
                documentation: `Interface: ${fullyQualifiedName}`,
                range: new monaco.Range(0, 0, 0, 0), // Adjust dynamically
            });

            // Check for methods inside the interface
            node.members.forEach(member => {
                if (ts.isMethodSignature(member)) {
                    const methodName = member.name ? member.name.getText() : 'anonymousMethod';
                    suggestions.push({
                        label: `${fullyQualifiedName}.${methodName}`,
                        kind: monaco.languages.CompletionItemKind.Method,
                        insertText: `${methodName}()`,
                        documentation: `Method: ${methodName}`,
                        range: new monaco.Range(0, 0, 0, 0), // Adjust dynamically
                    });
                }
            });
        }

        // Class declarations (including methods inside classes)
        else if (ts.isClassDeclaration(node)) {
            const fullyQualifiedName = getFullyQualifiedName(node);
            suggestions.push({
                label: fullyQualifiedName,
                kind: monaco.languages.CompletionItemKind.Class,
                insertText: fullyQualifiedName,
                documentation: `Class: ${fullyQualifiedName}`,
                range: new monaco.Range(0, 0, 0, 0), // Adjust dynamically
            });

            // Check for methods inside the class
            node.members.forEach(member => {
                if (ts.isMethodDeclaration(member)) {
                    const methodName = member.name ? member.name.getText() : 'anonymousMethod';
                    suggestions.push({
                        label: `${fullyQualifiedName}.${methodName}`,
                        kind: monaco.languages.CompletionItemKind.Method,
                        insertText: `${methodName}()`,
                        documentation: `Method: ${methodName}`,
                        range: new monaco.Range(0, 0, 0, 0), // Adjust dynamically
                    });
                }
            });
        }

        // Recursively visit all child nodes in the AST
        ts.forEachChild(node, visitNode);
    }

    // Start traversing from the root of the AST
    visitNode(sourceFile);

    return suggestions;
}

export function registerAemCodeCompletions(instance: Monaco) {
    fetch('/apps/contentor/api/assist-code/aem.d.ts')
        .then(response => response.text())
        .then(fileContent => {
            // Parse the file content to get suggestions
            const suggestions = parseDTSFile(fileContent);

            // Register the completion provider for Groovy language
            instance.languages.registerCompletionItemProvider('groovy', {
                provideCompletionItems: (model: monaco.editor.ITextModel, position: monaco.Position) => {
                    // Get the word at the cursor position
                    const wordRange = model.getWordAtPosition(position);
                    if (!wordRange) {
                        return { suggestions };
                    }

                    // Adjust the range based on the current word at the cursor
                    const adjustedSuggestions = suggestions.map(suggestion => ({
                        ...suggestion,
                        range: new monaco.Range(
                            position.lineNumber,
                            wordRange.startColumn,
                            position.lineNumber,
                            wordRange.endColumn
                        ),
                    }));

                    // Return the adjusted suggestions
                    return { suggestions: adjustedSuggestions };
                }
            });
        })
        .catch(error => {
            console.error('Error loading aem.d.ts:', error);
        });
}
