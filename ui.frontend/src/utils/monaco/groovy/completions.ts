import { Monaco } from "@monaco-editor/react";
import {registerSelfCodeCompletions} from "./completions/self-code.ts";
import {registerAemCodeCompletions} from "./completions/aem-code.ts";

export function registerCompletions(instance: Monaco) {
    registerSelfCodeCompletions(instance);
    registerAemCodeCompletions(instance);
}
