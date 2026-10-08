import { SCRIPT_API, methodSignature, type ApiLifecycleMethod, type ApiMethod } from './api';
import type { ScriptType } from '../domain/script';

export interface CatalogEntry {
  name: string;
  signature: string;
  docs: string;
  snippet?: string;
  deprecated?: boolean;
}

// Prose and snippets only; names and return types come from the generated API. Snippet parameters are untyped to need no imports.
const LIFECYCLE_DOCS: Record<string, { docs: string; snippet: string; params?: string }> = {
  describeRun: {
    docs: 'Declares inputs, e.g. `inputs.string(\'name\')`, and per-script settings such as `context.lockTimeout`.',
    snippet: 'void describeRun() {\n\t$0\n}',
  },
  canRun: {
    docs: 'Decides whether the script runs, e.g. `conditions.always()` for manual scripts or `conditions.changed()` for automatic ones.',
    snippet: 'boolean canRun() {\n\treturn ${1:conditions.always()}\n}',
  },
  doRun: {
    docs: 'Script body executed by ACM.',
    snippet: 'void doRun() {\n\t$0\n}',
  },
  scheduleRun: {
    docs: 'Schedule of an automatic script, e.g. `schedules.cron(\'0 0 2 ? * * *\')`.',
    snippet: "def scheduleRun() {\n\treturn ${1:schedules.cron('${2:0 0 2 ? * * *}')}\n}",
  },
  prepareRun: {
    docs: "Runs before every execution, e.g. to add variables to scripts with `context.variable('name', value)`.",
    params: 'ExecutionContext context',
    snippet: 'void prepareRun(context) {\n\t$0\n}',
  },
  completeRun: {
    docs: 'Runs after every execution, e.g. to react to a failed one with `execution.status`.',
    params: 'Execution execution',
    snippet: 'void completeRun(execution) {\n\t$0\n}',
  },
  prepareMock: {
    docs: 'Adds variables to mock scripts, like `prepareRun` does for other scripts.',
    params: 'MockContext context',
    snippet: 'void prepareMock(context) {\n\t$0\n}',
  },
  request: {
    docs: 'Tells whether the script answers the request, e.g. by method and URI. The first mock returning `true` responds.',
    params: 'HttpServletRequest request',
    snippet: "boolean request(request) {\n\treturn request.method == '${1:GET}' && request.requestURI == '${2:/mock/}'\n}",
  },
  respond: {
    docs: 'Writes the response of a matched request.',
    params: 'HttpServletRequest request, HttpServletResponse response',
    snippet: "void respond(request, response) {\n\tresponse.contentType = '${1:application/json}'\n\t$0\n}",
  },
  fail: {
    docs: 'Responds when a mock threw an exception; belongs in `core/fail.groovy`.',
    params: 'HttpServletRequest request, HttpServletResponse response, Exception exception',
    snippet: 'void fail(request, response, exception) {\n\t$0\n}',
  },
};

const BINDING_DOCS: Record<string, string> = {
  acl: 'Idempotent users, groups and permissions management.',
  activator: 'Content replication (activation, deactivation).',
  conditions: 'Conditions used in `canRun()`.',
  context: 'Current execution: id, abort checks, lock timeout, custom variables.',
  formatter: 'JSON, YAML, Base64 and template formatting.',
  inputs: 'Inputs declared in `describeRun()`, read in `doRun()` with `inputs.value(\'name\')`.',
  log: 'Logger writing to the console and AEM logs.',
  notifier: 'Notifications to Slack, Microsoft Teams and other configured notifiers.',
  osgi: 'Access to OSGi services.',
  out: 'Console output with levels: `out.info`, `out.success`, `out.warn`, `out.error`.',
  outputs: 'Outputs such as downloadable files and texts.',
  repo: 'Repository operations with auto-commit and dry-run support.',
  resourceResolver: 'Sling resource resolver of the executing user. Prefer `repo`.',
  schedules: 'Schedules used in `scheduleRun()`.',
};

function lifecycleEntries(methods: ApiLifecycleMethod[]): CatalogEntry[] {
  return methods.map((m) => ({
    name: m.name,
    signature: `${m.returns.split('.').pop()} ${m.name}(${LIFECYCLE_DOCS[m.name]?.params ?? ''})`,
    docs: `${LIFECYCLE_DOCS[m.name]?.docs ?? ''}${m.required ? ' Required.' : ''}`.trim(),
    snippet: LIFECYCLE_DOCS[m.name]?.snippet,
  }));
}

export const LIFECYCLE_METHODS = {
  content: lifecycleEntries(SCRIPT_API.lifecycle.content),
  extension: lifecycleEntries(SCRIPT_API.lifecycle.extension),
  mock: lifecycleEntries(SCRIPT_API.lifecycle.mock),
};

/** Methods a script of the type defines; scripts outside the scripts root (console, untitled) are content scripts. */
export function lifecycleMethodsFor(type?: ScriptType): CatalogEntry[] {
  return type === 'EXTENSION' ? LIFECYCLE_METHODS.extension : type === 'MOCK' ? LIFECYCLE_METHODS.mock : LIFECYCLE_METHODS.content;
}

export const BINDINGS: CatalogEntry[] = SCRIPT_API.bindings.map((b) => ({
  name: b.name,
  signature: `${b.type} ${b.name}`,
  docs: BINDING_DOCS[b.name] ?? '',
  deprecated: b.deprecated,
}));

function memberSnippet(method: ApiMethod): string {
  const closure = method.params.some((p) => p.type.startsWith('Closure'));
  const args = method.params.filter((p) => !p.type.startsWith('Closure'));
  const placeholders = args.map((p, i) => (p.type === 'String' ? `'\${${i + 1}:${p.name}}'` : `\${${i + 1}:${p.name}}`));
  return `${method.name}(${placeholders.join(', ')})${closure ? ' {\n\t$0\n}' : ''}`;
}

/** Methods of the variable's class, one entry per name with all overloads in the signature. */
export function bindingMembers(binding: string): CatalogEntry[] {
  const fqn = SCRIPT_API.bindings.find((b) => b.name === binding)?.class;
  const cls = fqn ? SCRIPT_API.classes[fqn] : undefined;
  if (!cls) return [];
  const byName = new Map<string, ApiMethod[]>();
  for (const m of cls.methods) byName.set(m.name, [...(byName.get(m.name) ?? []), m]);
  return [...byName.entries()].map(([name, overloads]) => {
    const richest = overloads.reduce((a, b) => (b.params.length > a.params.length ? b : a));
    return {
      name,
      signature: overloads.map(methodSignature).join('\n'),
      docs: overloads.find((m) => m.doc)?.doc ?? '',
      snippet: memberSnippet(richest),
      deprecated: overloads.every((m) => m.deprecated),
    };
  });
}
