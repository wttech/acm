import { Monaco } from "@monaco-editor/react";
import {registerSyntax} from "./groovy/syntax.ts";
import {registerCommands} from "./groovy/commands.ts";
import {registerCompletions} from "./groovy/completions.ts";
import {registerCodeActions} from "./groovy/code-actions.ts";

export const LANGUAGE_ID = 'groovy';

export function registerGroovyLanguage(instance: Monaco) {
    const languages = instance.languages.getLanguages();
    const registered = languages.some(lang => lang.id === LANGUAGE_ID);

    if (!registered) {
        registerSyntax(instance);
        registerCommands(instance);
        registerCompletions(instance);
        registerCodeActions(instance);
    }
}
