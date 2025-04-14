import { Button, Text } from '@adobe/react-spectrum';
import { ToastQueue } from '@react-spectrum/toast';
import Cancel from '@spectrum-icons/workflow/Cancel';
import React, { useState } from 'react';
import { pollExecutionPending } from '../hooks/execution';
import { apiRequest } from '../utils/api';
import { Execution, ExecutionStatus, isExecutionPending, QueueOutput } from '../utils/api.types';
import { ToastTimeoutQuick } from '../utils/spectrum.ts';

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
        url: `/apps/acm/api/queue-code.json?executionId=${execution.id}`,
        method: 'delete',
      });

      const queuedExecution = await pollExecutionPending(execution.id);
      if (queuedExecution?.status === ExecutionStatus.ABORTED) {
        ToastQueue.positive('Code execution aborted successfully!', {
          timeout: ToastTimeoutQuick,
        });
      } else {
        console.warn('Code execution aborting failed!');
        ToastQueue.negative('Code execution aborting failed!', {
          timeout: ToastTimeoutQuick,
        });
      }

      onComplete(queuedExecution);
    } catch (error) {
      console.error('Code execution aborting error:', error);
      ToastQueue.negative('Code execution aborting failed!', {
        timeout: ToastTimeoutQuick,
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
