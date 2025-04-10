import { ToastQueue } from '@react-spectrum/toast';
import { useState } from 'react';
import { useInterval } from 'react-use';
import { apiRequest } from '../utils/api';
import { Execution, ExecutionStatus, isExecutionPending, QueueOutput } from '../utils/api.types';
import { useFormatter } from './formatter';

const toastTimeout = 3000;

export const useExecutionPolling = (executionId: string | undefined | null, pollInterval: number = 900) => {
  const [execution, setExecution] = useState<Execution | null>(null);
  const [executing, setExecuting] = useState<boolean>(!!executionId);
  const [loading, setLoading] = useState<boolean>(true);
  const [wasPending, setWasPending] = useState<boolean>(false);
  const formatter = useFormatter();

  const pollExecutionState = async (executionId: string) => {
    try {
      const response = await apiRequest<QueueOutput>({
        operation: 'Code execution state',
        url: `/apps/acm/api/queue-code.json?executionId=${executionId}`,
        method: 'get',
      });
      const queuedExecution = response.data.data.executions.find((e: Execution) => e.id === executionId)!;
      setExecution(queuedExecution);
      setLoading(false);

      if (isExecutionPending(queuedExecution.status)) {
        setWasPending(true);
      } else {
        setExecuting(false);
        setWasPending(false);

        const recentlyCompleted = formatter.isRecent(queuedExecution.endDate, 2 * pollInterval);
        if (recentlyCompleted || wasPending) {
          if (queuedExecution.status === ExecutionStatus.FAILED) {
            ToastQueue.negative('Code execution failed!', { timeout: toastTimeout });
          } else if (queuedExecution.status === ExecutionStatus.SKIPPED) {
            ToastQueue.neutral('Code execution cannot run!', { timeout: toastTimeout });
          } else if (queuedExecution.status === ExecutionStatus.SUCCEEDED) {
            ToastQueue.positive('Code execution succeeded!', { timeout: toastTimeout });
          }
        }
      }
    } catch (error) {
      console.warn('Code execution state unknown:', error);
      setLoading(false);
    }
  };

  useInterval(
    () => {
      if (executing && executionId) {
        pollExecutionState(executionId);
      }
    },
    executing && executionId ? pollInterval : null,
  );

  return { execution, setExecution, executing, setExecuting, loading };
};
