import { Button, ButtonGroup, Content, Dialog, DialogTrigger, Divider, Heading, Text } from '@adobe/react-spectrum';
import Alert from '@spectrum-icons/workflow/Alert';
import Cancel from '@spectrum-icons/workflow/Cancel';
import Checkmark from '@spectrum-icons/workflow/Checkmark';
import Info from '@spectrum-icons/workflow/Info';
import UploadToCloud from '@spectrum-icons/workflow/UploadToCloud';
import React, { useState } from 'react';
import { toastRequest } from '../utils/api';

type ScriptSynchronizeButtonProps = {
  selectedKeys: string[];
  onSync: () => void;
};

const ScriptsSyncButton: React.FC<ScriptSynchronizeButtonProps> = ({ selectedKeys, onSync }) => {
  const [syncDialogOpen, setSyncDialogOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const handleSyncConfirm = async () => {
    setIsLoading(true);
    try {
      await toastRequest({
        method: 'POST',
        url: `/apps/acm/api/script.json?action=sync`,
        operation: `Synchronize scripts`,
      });
      onSync();
    } catch (error) {
      console.error(`Synchronize scripts error:`, error);
    } finally {
      setIsLoading(false);
      setSyncDialogOpen(false);
    }
  };

  return (
    <DialogTrigger isOpen={syncDialogOpen} onOpenChange={setSyncDialogOpen}>
      <Button variant="primary" onPress={() => setSyncDialogOpen(true)} isDisabled={selectedKeys.length > 0}>
        <UploadToCloud />
        <Text>Synchronize</Text>
      </Button>
      <Dialog>
        <Heading>
          <Text>Confirmation</Text>
        </Heading>
        <Divider />
        <Content>
          <p>
            <Info size="XS" /> Synchronizing <strong>all scripts</strong> will ensure consistency between the author and publish instances.
          </p>
          <p>
            <Alert size="XS" /> Are you sure you want to continue? Some scripts may be immediately executed on publish instances after synchronization, depending on their configuration.
          </p>
        </Content>
        <ButtonGroup>
          <Button variant="secondary" onPress={() => setSyncDialogOpen(false)} isDisabled={isLoading}>
            <Cancel />
            <Text>Cancel</Text>
          </Button>
          <Button variant="negative" style="fill" onPress={handleSyncConfirm} isPending={isLoading}>
            <Checkmark />
            <Text>Confirm</Text>
          </Button>
        </ButtonGroup>
      </Dialog>
    </DialogTrigger>
  );
};

export default ScriptsSyncButton;
