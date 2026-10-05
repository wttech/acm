export interface CatalogEntry {
  name: string;
  signature: string;
  docs: string;
  snippet?: string;
}

export interface LifecycleMethod extends CatalogEntry {
  returnType: string;
  required: boolean;
}

// Mirrors core/.../code/script/ContentScriptSyntax.java.
export const LIFECYCLE_METHODS: LifecycleMethod[] = [
  {
    name: 'describeRun',
    returnType: 'void',
    required: false,
    signature: 'void describeRun()',
    docs: 'Declares script inputs and outputs, e.g. `inputs.string(\'name\')`.',
    snippet: 'void describeRun() {\n\t$0\n}',
  },
  {
    name: 'canRun',
    returnType: 'boolean',
    required: true,
    signature: 'boolean canRun()',
    docs: 'Decides whether the script should run, e.g. `conditions.always()`.',
    snippet: 'boolean canRun() {\n\treturn ${1:conditions.always()}\n}',
  },
  {
    name: 'doRun',
    returnType: 'void',
    required: true,
    signature: 'void doRun()',
    docs: 'Script body executed by ACM.',
    snippet: 'void doRun() {\n\t$0\n}',
  },
  {
    name: 'scheduleRun',
    returnType: 'dev.vml.es.acm.core.code.Schedule',
    required: false,
    signature: 'Schedule scheduleRun()',
    docs: 'Schedule of an automatic script, e.g. `schedules.cron(...)`.',
    snippet: 'Schedule scheduleRun() {\n\treturn ${1:schedules.cron(\'0 0 * * * ?\')}\n}',
  },
];
