export type ExecutionStatus =
  | 'QUEUED'
  | 'ACTIVE'
  | 'PARSING'
  | 'CHECKING'
  | 'RUNNING'
  | 'STOPPING'
  | 'STOPPED'
  | 'ABORTED'
  | 'SKIPPED'
  | 'LOCKED'
  | 'FAILED'
  | 'SUCCEEDED';

export interface Execution {
  id: string;
  userId?: string;
  status: ExecutionStatus | string;
  startDate?: string;
  endDate?: string;
  duration?: number;
  output?: string;
  error?: string | null;
  executable?: { id: string; content?: string };
  /** Summary format carries the executable ID only. */
  executableId?: string;
  inputs?: Record<string, unknown> | null;
  instance?: unknown;
  [key: string]: unknown;
}

export interface QueueOutput {
  executions: Execution[];
}

export interface ExecutionListOutput {
  list: Execution[];
  [key: string]: unknown;
}

export const EXECUTION_STATUSES: readonly ExecutionStatus[] = [
  'QUEUED',
  'ACTIVE',
  'PARSING',
  'CHECKING',
  'RUNNING',
  'STOPPING',
  'SKIPPED',
  'LOCKED',
  'STOPPED',
  'ABORTED',
  'FAILED',
  'SUCCEEDED',
];

export function executableIdOf(e: Execution): string | undefined {
  return e.executable?.id ?? e.executableId;
}

export interface ExecutionFilter {
  executableId?: string;
  statuses: string[];
}

export function isExecutionFiltered(filter: ExecutionFilter): boolean {
  return !!filter.executableId || filter.statuses.length > 0;
}

export function matchesExecutionFilter(e: Execution, filter: ExecutionFilter): boolean {
  return (
    (!filter.executableId || executableIdOf(e) === filter.executableId) &&
    (filter.statuses.length === 0 || filter.statuses.includes(e.status.toUpperCase()))
  );
}

/** Query parameters of the execution API that select executions before pagination applies. */
export function executionFilterParams(filter: ExecutionFilter): Array<[string, string]> {
  return [
    ...(filter.executableId ? [['executableId', filter.executableId] as [string, string]] : []),
    ...filter.statuses.map((status): [string, string] => ['status', status]),
  ];
}

const PENDING_STATUSES = new Set(['QUEUED', 'ACTIVE', 'PARSING', 'CHECKING', 'RUNNING', 'STOPPING']);

export function isPending(status: string | undefined): boolean {
  return !!status && PENDING_STATUSES.has(status.toUpperCase());
}

const FAILED_STATUSES = new Set(['FAILED', 'ABORTED', 'LOCKED']);

export function isFailed(status: string | undefined): boolean {
  return !!status && FAILED_STATUSES.has(status.toUpperCase());
}

function parseJsonContainer(text: string): object | undefined {
  try {
    const value = JSON.parse(text);
    return value !== null && typeof value === 'object' ? value : undefined;
  } catch {
    return undefined;
  }
}

/** `pretty` formats inputs and JSON output as indented JSON (for editors); the default is compact (for agents). */
export function summarizeExecution(e: Execution, consoleOutput?: string | null, pretty = false): string {
  const lines: string[] = [];
  const executableId = executableIdOf(e);
  lines.push(`Execution ID: ${e.id}`);
  lines.push(`Status:       ${e.status}`);
  if (executableId) lines.push(`Executable:   ${executableId}`);
  if (e.userId) lines.push(`User:         ${e.userId}`);
  if (e.startDate) lines.push(`Started:      ${e.startDate}`);
  if (e.endDate) lines.push(`Ended:        ${e.endDate}`);
  if (e.duration !== undefined) lines.push(`Duration:     ${e.duration} ms`);
  const inputs = Object.entries(e.inputs ?? {});
  if (inputs.length) {
    const text = pretty
      ? JSON.stringify(
          Object.fromEntries(inputs.map(([name, value]) => [name, (typeof value === 'string' && parseJsonContainer(value)) || value])),
          null,
          2,
        )
      : inputs.map(([name, value]) => `${name}: ${JSON.stringify(value)}`).join('\n');
    lines.push(`\n--- INPUTS ---\n${text}`);
  }
  const out = consoleOutput ?? e.output;
  if (out && out.trim()) {
    const json = pretty ? parseJsonContainer(out) : undefined;
    lines.push(`\n--- OUTPUT ---\n${json ? JSON.stringify(json, null, 2) : out.trimEnd()}`);
  }
  // Last, as in the run itself: a failing script prints first and fails afterwards.
  if (e.error) lines.push(`\n--- ERROR ---\n${e.error}`);
  return lines.join('\n');
}
