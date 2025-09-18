import { Button, ButtonGroup, Content, Dialog, DialogTrigger, Divider, Heading, Text } from '@adobe/react-spectrum';
import Alert from '@spectrum-icons/workflow/Alert';
import Cancel from '@spectrum-icons/workflow/Cancel';
import Checkmark from '@spectrum-icons/workflow/Checkmark';
import GearsDelete from '@spectrum-icons/workflow/GearsDelete';
import React, { useState } from 'react';
import { toastRequest } from '../utils/api.ts';
import { EventType, QueueOutput } from '../utils/api.types.ts';

type ExecutionsResetButtonProps = {
  onReset?: () => void;
};

const ExecutorResetButton: React.FC<ExecutionsResetButtonProps> = ({ onReset }) => {
  const [resetDialogOpen, setResetDialogOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const handleConfirm = async () => {
    setIsLoading(true);
    try {
      await toastRequest<QueueOutput>({
        method: 'POST',
        url: `/apps/acm/api/event.json?name=${EventType.EXECUTOR_RESET}`,
        operation: 'Reset executions',
      });
      if (onReset) onReset();
    } catch (error) {
      console.error('Reset executions error:', error);
    } finally {
      setIsLoading(false);
      setResetDialogOpen(false);
    }
  };

  const renderResetDialog = () => (
    <>
      <Heading>
        <Text>Confirmation</Text>
      </Heading>
      <Divider />
      <Content>
        <Text>
          <p>
            Are you sure you want to reset the executor? This action will <strong>affect all instances</strong>, immediately stop any ongoing executions, clear all jobs from the Sling queue, and clear repository locks.
            <br />
          </p>
          <p>
            <Alert size="XS" /> This action cannot be undone.
          </p>
        </Text>
      </Content>
      <ButtonGroup>
        <Button variant="secondary" onPress={() => setResetDialogOpen(false)} isDisabled={isLoading}>
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
    <DialogTrigger isOpen={resetDialogOpen} onOpenChange={setResetDialogOpen}>
      <Button variant="secondary" style="fill" onPress={() => setResetDialogOpen(true)}>
        <GearsDelete />
        <Text>Reset</Text>
      </Button>
      <Dialog>{renderResetDialog()}</Dialog>
    </DialogTrigger>
  );
};

export default ExecutorResetButton;
