import { Button, ButtonGroup, Content, Dialog, DialogTrigger, Divider, Heading, InlineAlert, Text } from '@adobe/react-spectrum';
import { ToastQueue } from '@react-spectrum/toast';
import AlertIcon from '@spectrum-icons/workflow/Alert';
import Cancel from '@spectrum-icons/workflow/Cancel';
import CheckmarkCircle from '@spectrum-icons/workflow/CheckmarkCircle';
import CloseCircle from '@spectrum-icons/workflow/CloseCircle';
import StopCircle from '@spectrum-icons/workflow/StopCircle';
import React, { useState } from 'react';
import { useDeepCompareEffect } from 'react-use';
import { useAppState } from '../hooks/app.ts';
import { pollExecutionPending } from '../hooks/execution';
import { Execution, ExecutionStatus, isExecutionPending } from '../types/execution.ts';
import { QueueOutput } from '../types/main.ts';
import { apiRequest } from '../utils/api';
import { ToastTimeoutQuick } from '../utils/spectrum.ts';

interface ExecutionAbortButtonProps {
  execution: Execution | null;
  onComplete: (execution: Execution | null) => void;
}

const ExecutionAbortButton: React.FC<ExecutionAbortButtonProps> = ({ execution, onComplete }) => {
  const appState = useAppState();

  const [showDialog, setShowDialog] = useState(false);
  const [isAborting, setIsAborting] = useState(false);

  // Auto-close dialog if execution is no longer pending
  useDeepCompareEffect(() => {
    if (showDialog && (!execution || !isExecutionPending(execution.status))) {
      setShowDialog(false);
    }
  }, [execution, showDialog]);

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

      const queuedExecution = await pollExecutionPending(execution.id, appState.spaSettings.executionPollInterval);
      if (queuedExecution?.status === ExecutionStatus.ABORTED) {
        ToastQueue.positive('Code execution aborted successfully!', {
          timeout: ToastTimeoutQuick,
        });
      } else {
        console.warn('Code execution aborting failed!', queuedExecution);
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
      setShowDialog(false);
    }
  };

  return (
    <DialogTrigger isOpen={showDialog} onOpenChange={setShowDialog}>
      <Button variant="negative" isDisabled={!execution || !isExecutionPending(execution.status) || isAborting} onPress={() => setShowDialog(true)}>
        <StopCircle />
        <Text>Abort</Text>
      </Button>
      <Dialog>
        <Heading>
          <Text>Confirmation</Text>
        </Heading>
        <Divider />
        <Content>
          <p>
            <CheckmarkCircle size="XS" /> The abort request signals the script to stop, but the script must explicitly check for this signal by calling <code>context.checkAborted()</code>.
          </p>
          <p>
            <CloseCircle size="XS" /> If the script doesn't check for abort, it will continue running until it completes naturally. Only if an abort timeout is configured (by default it's not), will the execution be forcefully terminated after the timeout expires.
          </p>
          <p>
            <AlertIcon size="XS" /> For scripts with loops or long-running operations, add <code>context.checkAborted()</code> at safe checkpoints (e.g., at the beginning of each loop iteration) to enable graceful termination and prevent data corruption.
          </p>

          <InlineAlert width="100%" variant="negative" UNSAFE_style={{ padding: '8px' }} marginTop="size-200">
            <Heading>Warning</Heading>
            <Content>
              Proceed with aborting only if the requirements above are met.<br/>
              This action cannot be undone.
            </Content>
          </InlineAlert>
        </Content>
        <ButtonGroup>
          <Button variant="secondary" onPress={() => setShowDialog(false)} isDisabled={isAborting}>
            <Cancel />
            <Text>Cancel</Text>
          </Button>
          <Button variant="negative" style="fill" onPress={onAbort} isPending={isAborting}>
            <StopCircle />
            <Text>Abort</Text>
          </Button>
        </ButtonGroup>
      </Dialog>
    </DialogTrigger>
  );
};

export default ExecutionAbortButton;
