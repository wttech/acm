import { Button, Text } from '@adobe/react-spectrum';
import { ToastQueue } from '@react-spectrum/toast';
import Cancel from '@spectrum-icons/workflow/Cancel';
import React, { useState } from 'react';
import { apiRequest } from '../utils/api';
import { Execution, ExecutionStatus, isExecutionPending, QueueOutput } from '../utils/api.types';

const executionPollInterval = 1000;
const toastTimeout = 3000;

interface ExecutionAbortButtonProps {
  execution: Execution | null;
  onComplete: (execution: Execution | null) => void;
}

const ExecutionAbortButton: React.FC<ExecutionAbortButtonProps> = ({ execution, onComplete }) => {
  const [isAborting, setIsAborting] = useState(false);

  const onAbort = async () => {
    if (!execution?.id) {
      console.warn('Code execution cannot be aborted as it is not running!');
      return;
    }
    setIsAborting(true);
    try {
      await apiRequest<QueueOutput>({
        operation: 'Code execution aborting',
        url: `/apps/acm/api/queue-code.json?jobId=${execution.id}`,
        method: 'delete',
      });

      let queuedExecution: Execution | null = null;
      while (queuedExecution === null || isExecutionPending(queuedExecution.status)) {
        const response = await apiRequest<QueueOutput>({
          operation: 'Code execution state',
          url: `/apps/acm/api/queue-code.json?jobId=${execution.id}`,
          method: 'get',
        });
        queuedExecution = response.data.data.executions[0]!;
        onComplete(queuedExecution);
        await new Promise((resolve) => setTimeout(resolve, executionPollInterval));
      }
      if (queuedExecution.status === ExecutionStatus.ABORTED) {
        ToastQueue.positive('Code execution aborted successfully!', {
          timeout: toastTimeout,
        });
      } else {
        console.warn('Code execution aborting failed!');
        ToastQueue.negative('Code execution aborting failed!', {
          timeout: toastTimeout,
        });
      }
    } catch (error) {
      console.error('Code execution aborting error:', error);
      ToastQueue.negative('Code execution aborting failed!', {
        timeout: toastTimeout,
      });
    } finally {
      setIsAborting(false);
    }
  };

  return (
    <Button variant="negative" isDisabled={!execution || !isExecutionPending(execution.status) || isAborting} onPress={onAbort}>
      <Cancel />
      <Text>Abort</Text>
    </Button>
  );
};

export default ExecutionAbortButton;
