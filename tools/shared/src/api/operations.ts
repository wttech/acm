import { AcmHttpError, type AcmClient } from './client';
import { ACM_API } from './paths';
import type {
  Execution,
  ExecutionFilter,
  ExecutionListOutput,
  QueueOutput,
} from '../domain/execution';
import { executionFilterParams, isPending, matchesExecutionFilter } from '../domain/execution';

/** Lists executions still queued or running, then history (newest first); the filter applies before the history limit. */
export async function fetchExecutions(
  client: AcmClient,
  limit: number,
  filter: ExecutionFilter = { statuses: [] },
): Promise<Execution[]> {
  const list = async (params: Array<[string, string]>) => {
    const query = new URLSearchParams([['format', 'summary'], ...params]);
    return (await client.request<ExecutionListOutput>('GET', `${ACM_API.execution}?${query}`)).data?.list ?? [];
  };
  const [queued, history] = await Promise.all([
    list([['queued', 'true']]).then((all) => all.filter((e) => matchesExecutionFilter(e, filter))),
    list([['limit', String(limit)], ...executionFilterParams(filter)]),
  ]);
  return [...queued, ...history.filter((e) => !queued.some((q) => q.id === e.id))];
}

/** Finds an execution in the queue (pending or just finished) or, failing that, in history. */
export async function fetchExecutionById(client: AcmClient, executionId: string): Promise<Execution | null> {
  try {
    const q = await client.request<QueueOutput>(
      'GET',
      `${ACM_API.queueCode}?executionId=${encodeURIComponent(executionId)}`,
    );
    const found = q.data?.executions?.find((e) => e.id === executionId);
    if (found) return found;
  } catch (e) {
    if (!(e instanceof AcmHttpError && e.httpStatus === 404)) throw e;
  }
  try {
    const h = await client.request<ExecutionListOutput>(
      'GET',
      `${ACM_API.execution}?id=${encodeURIComponent(executionId)}&format=full`,
    );
    const list = h.data?.list || [];
    return list.find((e) => e.id === executionId) || list[0] || null;
  } catch (e) {
    if (e instanceof AcmHttpError && e.httpStatus === 404) return null;
    throw e;
  }
}

export async function fetchConsoleOutput(client: AcmClient, executionId: string): Promise<string | null> {
  try {
    const r = await client.requestRaw(
      `${ACM_API.executionOutput}?executionId=${encodeURIComponent(executionId)}&name=console`,
    );
    return r.status === 200 ? r.text : null;
  } catch {
    return null;
  }
}

export interface WaitOptions {
  intervalMs: number;
  /** Stops waiting after this long and returns the execution as last seen; waits until it finishes when omitted. */
  timeoutMs?: number;
  /** Called with the execution before each wait, e.g. to report progress. */
  onPoll?: (execution: Execution) => void;
}

/** Polls an execution until it is no longer pending or the timeout passes. */
export async function waitForExecution(
  client: AcmClient,
  execution: Execution,
  options: WaitOptions,
): Promise<Execution> {
  const deadline = options.timeoutMs === undefined ? Infinity : Date.now() + options.timeoutMs;
  let current = execution;
  while (isPending(current.status) && Date.now() < deadline) {
    options.onPoll?.(current);
    await new Promise((resolve) => setTimeout(resolve, options.intervalMs));
    current = (await fetchExecutionById(client, current.id)) ?? current;
  }
  return current;
}
