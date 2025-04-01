import { useState } from 'react';
import { useInterval } from 'react-use';
import { ToastQueue } from '@react-spectrum/toast';
import { apiRequest } from '../utils/api';
import { Execution, ExecutionStatus, isExecutionPending, QueueOutput } from '../utils/api.types';

const toastTimeout = 3000;

export const useExecutionPolling = (jobId: string | undefined | null, pollInterval: number = 500) => {
    const [execution, setExecution] = useState<Execution | null>(null);
    const [executing, setExecuting] = useState<boolean>(!!jobId);

    const pollExecutionState = async (jobId: string) => {
        try {
            const response = await apiRequest<QueueOutput>({
                operation: 'Code execution state',
                url: `/apps/acm/api/queue-code.json?jobId=${jobId}`,
                method: 'get',
            });
            const queuedExecution = response.data.data.executions.find((e: Execution) => e.id === jobId)!;
            setExecution(queuedExecution);

            if (!isExecutionPending(queuedExecution.status)) {
                setExecuting(false);
                if (queuedExecution.status === ExecutionStatus.FAILED) {
                    ToastQueue.negative('Code execution failed!', { timeout: toastTimeout });
                } else if (queuedExecution.status === ExecutionStatus.SKIPPED) {
                    ToastQueue.neutral('Code execution cannot run!', { timeout: toastTimeout });
                } else if (queuedExecution.status === ExecutionStatus.SUCCEEDED) {
                    ToastQueue.positive('Code execution succeeded!', { timeout: toastTimeout });
                }
            }
        } catch (error) {
            console.warn('Code execution state unknown:', error);
        }
    };

    useInterval(
        () => {
            if (executing && jobId) {
                pollExecutionState(jobId);
            }
        },
        executing && jobId ? pollInterval : null,
    );

    return { execution, setExecution, executing, setExecuting };
};