import { SCRIPT_API, methodSignature, type ApiMethod } from './api';

export interface CatalogEntry {
  name: string;
  signature: string;
  docs: string;
  snippet?: string;
  deprecated?: boolean;
}

// Prose and snippets only; names, types and signatures come from the generated API.
const LIFECYCLE_DOCS: Record<string, { docs: string; snippet: string }> = {
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

export const LIFECYCLE_METHODS: CatalogEntry[] = SCRIPT_API.lifecycle.content.map((m) => ({
  name: m.name,
  signature: `${m.returns.split('.').pop()} ${m.name}()`,
  docs: `${LIFECYCLE_DOCS[m.name]?.docs ?? ''}${m.required ? ' Required.' : ''}`.trim(),
  snippet: LIFECYCLE_DOCS[m.name]?.snippet,
}));

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
