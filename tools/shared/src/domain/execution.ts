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

const PENDING_STATUSES = new Set(['QUEUED', 'ACTIVE', 'PARSING', 'CHECKING', 'RUNNING', 'STOPPING']);

export function isPending(status: string | undefined): boolean {
  return !!status && PENDING_STATUSES.has(status.toUpperCase());
}

const FAILED_STATUSES = new Set(['FAILED', 'ABORTED', 'LOCKED']);

export function isFailed(status: string | undefined): boolean {
  return !!status && FAILED_STATUSES.has(status.toUpperCase());
}

export function summarizeExecution(e: Execution, consoleOutput?: string | null): string {
  const lines: string[] = [];
  const executableId = e.executable?.id ?? e.executableId;
  lines.push(`Execution ID: ${e.id}`);
  lines.push(`Status:       ${e.status}`);
  if (executableId) lines.push(`Executable:   ${executableId}`);
  if (e.userId) lines.push(`User:         ${e.userId}`);
  if (e.startDate) lines.push(`Started:      ${e.startDate}`);
  if (e.endDate) lines.push(`Ended:        ${e.endDate}`);
  if (e.duration !== undefined) lines.push(`Duration:     ${e.duration} ms`);
  const inputs = Object.entries(e.inputs ?? {});
  if (inputs.length) {
    lines.push(`\n--- INPUTS ---\n${inputs.map(([name, value]) => `${name}: ${JSON.stringify(value)}`).join('\n')}`);
  }
  if (e.error) lines.push(`\n--- ERROR ---\n${e.error}`);
  const out = consoleOutput ?? e.output;
  if (out && out.trim()) lines.push(`\n--- OUTPUT ---\n${out}`);
  return lines.join('\n');
}
