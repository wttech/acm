import { Button, ButtonGroup, Content, Dialog, DialogTrigger, Divider, Heading, Text } from '@adobe/react-spectrum';
import Alert from '@spectrum-icons/workflow/Alert';
import Cancel from '@spectrum-icons/workflow/Cancel';
import Checkmark from '@spectrum-icons/workflow/Checkmark';
import DataRemove from '@spectrum-icons/workflow/DataRemove';
import Flashlight from '@spectrum-icons/workflow/Flashlight';
import React, { useState } from 'react';
import { toastRequest } from '../utils/api';
import { EventType, QueueOutput } from '../utils/api.types.ts';

type ExecutionHistoryClearButtonProps = {
  onClear?: () => void;
};

const ExecutionHistoryClearButton: React.FC<ExecutionHistoryClearButtonProps> = ({ onClear }) => {
  const [dialogOpen, setDialogOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const handleConfirm = async () => {
    setIsLoading(true);
    try {
      await toastRequest<QueueOutput>({
        method: 'POST',
        url: `/apps/acm/api/event.json?name=${EventType.HISTORY_CLEAR}`,
        operation: 'Clear execution history',
      });
      if (onClear) onClear();
    } catch (error) {
      console.error('Clear execution history error:', error);
    } finally {
      setIsLoading(false);
      setDialogOpen(false);
    }
  };

  const renderDialog = () => (
    <>
      <Heading>
        <Text>Confirmation</Text>
      </Heading>
      <Divider />
      <Content>
        <Text>
          <p>
            Are you sure you want to clear the execution history? This will remove all past executions <b>for all instances</b>.<br />
          </p>
          <p>
            <Flashlight size="XS" /> Do not clear execution history <b>too often</b> &mdash; it may be unexpectedly useful for auditing or troubleshooting in the future.
          </p>
          <p>
            <Alert size="XS" /> This action cannot be undone.
          </p>
        </Text>
      </Content>
      <ButtonGroup>
        <Button variant="secondary" onPress={() => setDialogOpen(false)} isDisabled={isLoading}>
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
    <DialogTrigger isOpen={dialogOpen} onOpenChange={setDialogOpen}>
      <Button variant="negative" onPress={() => setDialogOpen(true)}>
        <DataRemove />
        <Text>Clear</Text>
      </Button>
      <Dialog>{renderDialog()}</Dialog>
    </DialogTrigger>
  );
};

export default ExecutionHistoryClearButton;
