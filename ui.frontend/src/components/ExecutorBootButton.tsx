import { Button, ButtonGroup, Content, Dialog, DialogTrigger, Divider, Heading, Text } from '@adobe/react-spectrum';
import Alert from '@spectrum-icons/workflow/Alert';
import Cancel from '@spectrum-icons/workflow/Cancel';
import Checkmark from '@spectrum-icons/workflow/Checkmark';
import Launch from '@spectrum-icons/workflow/Launch';
import React, { useState } from 'react';
import { toastRequest } from '../utils/api.ts';
import { EventType, QueueOutput } from '../types/main.ts';

type ExecutionsBootButtonProps = {
  onBoot?: () => void;
};

const ExecutorBootButton: React.FC<ExecutionsBootButtonProps> = ({ onBoot }) => {
  const [bootDialogOpen, setBootDialogOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const handleConfirm = async () => {
    setIsLoading(true);
    try {
      await toastRequest<QueueOutput>({
        method: 'POST',
        url: `/apps/acm/api/event.json?name=${EventType.SCRIPT_SCHEDULER_BOOT}`,
        operation: 'Boot script scheduler',
      });
      if (onBoot) onBoot();
    } catch (error) {
      console.error('Boot script scheduler error:', error);
    } finally {
      setIsLoading(false);
      setBootDialogOpen(false);
    }
  };

  const renderBootDialog = () => (
    <>
      <Heading>
        <Text>Confirmation</Text>
      </Heading>
      <Divider />
      <Content>
        <Text>
          <p>
            Are you sure you want to force boot of the script scheduler? This may be useful e.g. when the automatic boot during deployment has reached a timeout.
            <br />
          </p>
          <p>
            <Alert size="XS" /> This action will <strong>affect all instances</strong>.
          </p>
        </Text>
      </Content>
      <ButtonGroup>
        <Button variant="secondary" onPress={() => setBootDialogOpen(false)} isDisabled={isLoading}>
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
    <DialogTrigger isOpen={bootDialogOpen} onOpenChange={setBootDialogOpen}>
      <Button variant="secondary" style="outline" onPress={() => setBootDialogOpen(true)}>
        <Launch />
        <Text>Boot</Text>
      </Button>
      <Dialog>{renderBootDialog()}</Dialog>
    </DialogTrigger>
  );
};

export default ExecutorBootButton;
