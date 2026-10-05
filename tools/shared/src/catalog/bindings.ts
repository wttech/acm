import type { CatalogEntry } from './lifecycle';

// Mirrors variables bound in core/.../code/CodeContext.java and ExecutionContext.java.
// TODO: replace short docs with the ones from ACM documentation.
export const BINDINGS: CatalogEntry[] = [
  { name: 'context', signature: 'ExecutionContext context', docs: 'Current execution context.' },
  { name: 'inputs', signature: 'Inputs inputs', docs: 'Script inputs; declare in `describeRun()`, read in `doRun()`.' },
  { name: 'outputs', signature: 'Outputs outputs', docs: 'Script outputs such as files and texts.' },
  { name: 'conditions', signature: 'Conditions conditions', docs: 'Conditions used in `canRun()`.' },
  { name: 'schedules', signature: 'Schedules schedules', docs: 'Schedules used in `scheduleRun()`.' },
  { name: 'out', signature: 'PrintStream out', docs: 'Console output.' },
  { name: 'log', signature: 'Logger log', docs: 'SLF4J logger.' },
  { name: 'resourceResolver', signature: 'ResourceResolver resourceResolver', docs: 'Sling resource resolver of the executing user.' },
  { name: 'repo', signature: 'Repo repo', docs: 'JCR repository helper.' },
  { name: 'acl', signature: 'Acl acl', docs: 'Access control helper.' },
  { name: 'osgi', signature: 'OsgiContext osgi', docs: 'OSGi services access.' },
  { name: 'formatter', signature: 'Formatter formatter', docs: 'Value formatting helper.' },
  { name: 'activator', signature: 'Activator activator', docs: 'Content replication helper.' },
  { name: 'notifier', signature: 'Notifier notifier', docs: 'Notification helper.' },
];
