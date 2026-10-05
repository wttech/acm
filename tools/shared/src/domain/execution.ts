export type ExecutionStatus =
  | 'QUEUED'
  | 'ACTIVE'
  | 'PARSING'
  | 'CHECKING'
  | 'RUNNING'
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

const PENDING_STATUSES = new Set(['QUEUED', 'ACTIVE', 'PARSING', 'CHECKING', 'RUNNING']);

export function isPending(status: string | undefined): boolean {
  return !!status && PENDING_STATUSES.has(status.toUpperCase());
}

const FAILED_STATUSES = new Set(['FAILED', 'ABORTED', 'LOCKED']);

export function isFailed(status: string | undefined): boolean {
  return !!status && FAILED_STATUSES.has(status.toUpperCase());
}

export function summarizeExecution(e: Execution, consoleOutput?: string | null): string {
  const lines: string[] = [];
  lines.push(`Execution ID: ${e.id}`);
  lines.push(`Status:       ${e.status}`);
  if (e.executable?.id) lines.push(`Executable:   ${e.executable.id}`);
  if (e.userId) lines.push(`User:         ${e.userId}`);
  if (e.startDate) lines.push(`Started:      ${e.startDate}`);
  if (e.endDate) lines.push(`Ended:        ${e.endDate}`);
  if (e.duration !== undefined) lines.push(`Duration:     ${e.duration} ms`);
  if (e.error) lines.push(`\n--- ERROR ---\n${e.error}`);
  const out = consoleOutput ?? e.output;
  if (out && out.trim()) lines.push(`\n--- OUTPUT ---\n${out}`);
  return lines.join('\n');
}
