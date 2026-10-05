import { AcmHttpError, type AcmClient } from './client';
import { ACM_API } from './paths';
import type { Execution, ExecutionListOutput, QueueOutput } from '../domain/execution';

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
