import { Button, ButtonGroup, Content, Dialog, DialogTrigger, Divider, Heading, InlineAlert, Text } from '@adobe/react-spectrum';
import Cancel from '@spectrum-icons/workflow/Cancel';
import Checkmark from '@spectrum-icons/workflow/Checkmark';
import React, { useState } from 'react';
import { QueueOutput } from '../types/main.ts';
import { toastRequest } from '../utils/api';
import CheckmarkCircle from '@spectrum-icons/workflow/CheckmarkCircle';
import CloseCircle from '@spectrum-icons/workflow/CloseCircle';
import AlertIcon from '@spectrum-icons/workflow/Alert';

type ExecutionsAbortButtonProps = {
  selectedKeys: string[];
  onAbort?: () => void;
};

const ExecutionsAbortButton: React.FC<ExecutionsAbortButtonProps> = ({ selectedKeys, onAbort }) => {
  const [abortDialogOpen, setAbortDialogOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const handleConfirm = async () => {
    setIsLoading(true);
    const ids = Array.from(selectedKeys);

    const params = new URLSearchParams();
    ids.forEach((id) => params.append('executionId', id));

    try {
      await toastRequest<QueueOutput>({
        method: 'DELETE',
        url: `/apps/acm/api/queue-code.json?${params.toString()}`,
        operation: 'Abort executions',
      });
      if (onAbort) onAbort();
    } catch (error) {
      console.error('Abort executions error:', error);
    } finally {
      setIsLoading(false);
      setAbortDialogOpen(false);
    }
  };

  const renderAbortDialog = () => (
    <>
      <Heading>
        <Text>Confirmation</Text>
      </Heading>
      <Divider />
      <Content>
        <p><strong>Are you sure you want to abort the selected executions?</strong></p>

        <p>
          <CheckmarkCircle size="XS" /> The abort request signals scripts to stop, but scripts must explicitly check for this signal by calling <code>context.checkAborted()</code>.
        </p>
        <p>
          <CloseCircle size="XS" /> If scripts don't check for abort, they will continue running until they complete naturally. Only if an abort timeout is configured (by default it's not), will the execution be forcefully terminated after the timeout expires.
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
        <Button variant="secondary" onPress={() => setAbortDialogOpen(false)} isDisabled={isLoading}>
          <Cancel />
          <Text>Cancel</Text>
        </Button>
        <Button variant="negative" style="fill" onPress={handleConfirm} isPending={isLoading}>
          <Checkmark />
          <Text>Confirm</Text>
        </Button>
      </ButtonGroup>
    </>
  );

  return (
    <DialogTrigger isOpen={abortDialogOpen} onOpenChange={setAbortDialogOpen}>
      <Button variant="negative" style="fill" isDisabled={selectedKeys.length === 0} onPress={() => setAbortDialogOpen(true)}>
        <Cancel />
        <Text>Abort</Text>
      </Button>
      <Dialog>{renderAbortDialog()}</Dialog>
    </DialogTrigger>
  );
};

export default ExecutionsAbortButton;
