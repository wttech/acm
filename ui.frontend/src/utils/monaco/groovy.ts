import { Monaco } from "@monaco-editor/react";
import {registerSyntax} from "./groovy/syntax.ts";
import {registerCommands} from "./groovy/commands.ts";
import {registerCompletions} from "./groovy/completions.ts";
import {registerCodeActions} from "./groovy/code-actions.ts";

export function registerGroovyLanguage(instance: Monaco) {
    registerSyntax(instance);
    registerCommands(instance);
    registerCompletions(instance);
    registerCodeActions(instance);
}
