import { ToastQueue } from '@react-spectrum/toast';
import { useEffect, useRef, useState } from 'react';
import { useInterval } from 'react-use';
import { isExecutableScript } from '../types/executable';
import { Execution, ExecutionStatus, isExecutionPending } from '../types/execution';
import { QueueOutput } from '../types/main';
import { apiRequest } from '../utils/api';
import { intervalToTimeout, ToastTimeoutQuick } from '../utils/spectrum';
import { useAppState } from './app';
import { useFormatter } from './formatter';

export const useExecutionPolling = (executionId: string | undefined | null, pollInterval: number) => {
  const appState = useAppState();
  const [execution, setExecution] = useState<Execution | null>(null);
  const [executing, setExecuting] = useState<boolean>(!!executionId);
  const [loading, setLoading] = useState<boolean>(true);
  const [wasPending, setWasPending] = useState<boolean>(false);
  const [justCompleted, setJustCompleted] = useState<boolean>(false);
  const formatter = useFormatter();

  const pollExecutionState = async (executionId: string) => {
    try {
      const response = await apiRequest<QueueOutput>({
        operation: 'Code execution state',
        url: `/apps/acm/api/queue-code.json?executionId=${executionId}`,
        method: 'get',
        quiet: true,
        timeout: intervalToTimeout(pollInterval),
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
          setJustCompleted(true);
          if (queuedExecution.status === ExecutionStatus.FAILED) {
            ToastQueue.negative('Code execution failed!', { timeout: ToastTimeoutQuick });
          } else if (queuedExecution.status === ExecutionStatus.SKIPPED) {
            ToastQueue.neutral('Code execution skipped — conditions not met.', { timeout: ToastTimeoutQuick });
          } else if (queuedExecution.status === ExecutionStatus.LOCKED) {
            ToastQueue.neutral('Code execution locked — already running.', { timeout: ToastTimeoutQuick });
          } else if (queuedExecution.status === ExecutionStatus.SUCCEEDED) {
            ToastQueue.positive('Code execution succeeded!', { timeout: ToastTimeoutQuick });
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
    executing && executionId ? appState.spaSettings.executionPollInterval : null,
  );

  return { execution, setExecution, executing, setExecuting, loading, justCompleted };
};

// Signals a script execution just succeeded with a 'auto' review policy, exactly once per execution id
export const useExecutionReviewAutoOpen = (execution: Execution | null, justCompleted: boolean): boolean => {
  const appState = useAppState();
  const autoOpenedIdRef = useRef<string | null>(null);

  const autoOpen =
    !!execution &&
    appState.spaSettings.executionReviewOutputsPolicy === 'auto' &&
    isExecutableScript(execution.executable.id) &&
    execution.status === ExecutionStatus.SUCCEEDED &&
    justCompleted &&
    autoOpenedIdRef.current !== execution.id;

  useEffect(() => {
    if (autoOpen && execution) {
      autoOpenedIdRef.current = execution.id;
    }
  }, [autoOpen, execution]);

  return autoOpen;
};

export const pollExecutionPending = async (executionId: string, pollInterval: number): Promise<Execution> => {
  let queuedExecution: Execution | null = null;

  while (queuedExecution === null || isExecutionPending(queuedExecution.status)) {
    try {
      const response = await apiRequest<QueueOutput>({
        operation: 'Code execution pending state',
        url: `/apps/acm/api/queue-code.json?executionId=${executionId}`,
        method: 'get',
        quiet: true,
        timeout: intervalToTimeout(pollInterval),
      });
      queuedExecution = response.data.data.executions[0]!;
    } catch (error) {
      console.warn('Code execution pending state unknown:', error);
    }
    await new Promise((resolve) => setTimeout(resolve, pollInterval));
  }

  return queuedExecution!;
};
