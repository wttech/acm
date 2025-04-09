import { Button, ButtonGroup, Content, Dialog, DialogTrigger, Divider, Heading, Text } from '@adobe/react-spectrum';
import Cancel from '@spectrum-icons/workflow/Cancel';
import Checkmark from '@spectrum-icons/workflow/Checkmark';
import React, { useState } from 'react';
import { toastRequest } from '../utils/api';
import { ScriptType } from '../utils/api.types';

type ScriptToggleButtonProps = {
  type: ScriptType.ENABLED | ScriptType.DISABLED;
  selectedKeys: string[];
  onToggle: () => void;
};

const ScriptToggleButton: React.FC<ScriptToggleButtonProps> = ({ type, selectedKeys, onToggle }) => {
  const [toggleDialogOpen, setToggleDialogOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const handleConfirm = async () => {
    setIsLoading(true);
    const action = type === ScriptType.ENABLED ? 'disable' : 'enable';
    const ids = Array.from(selectedKeys);

    const params = new URLSearchParams();
    params.append('action', action);
    params.append('type', type);
    ids.forEach((id) => params.append('id', id));

    try {
      await toastRequest({
        method: 'POST',
        url: `/apps/acm/api/script.json?${params.toString()}`,
        operation: `${action} scripts`,
      });
      onToggle();
    } catch (error) {
      console.error(`${action} scripts error:`, error);
    } finally {
      setIsLoading(false);
      setToggleDialogOpen(false);
    }
  };

  const renderToggleDialog = () => (
    <>
      <Heading>
        <Text>Confirmation</Text>
      </Heading>
      <Divider />
      <Content>
        {type === ScriptType.ENABLED ? (
          <Text>Disabling scripts will stop their automatic execution. To execute them again, you need to enable them or reinstall the package with scripts.</Text>
        ) : (
          <Text>Enabling scripts can cause changes in the repository and potential data loss. Ensure the script is ready to use. It is recommended to provide enabled scripts via a package, not manually.</Text>
        )}
      </Content>
      <ButtonGroup>
        <Button variant="secondary" onPress={() => setToggleDialogOpen(false)} isDisabled={isLoading}>
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
    <DialogTrigger isOpen={toggleDialogOpen} onOpenChange={setToggleDialogOpen}>
      <Button variant={type === ScriptType.ENABLED ? 'negative' : 'accent'} style="fill" isDisabled={selectedKeys.length === 0} onPress={() => setToggleDialogOpen(true)}>
        {type === ScriptType.ENABLED ? <Cancel /> : <Checkmark />}
        <Text>{type === ScriptType.ENABLED ? 'Disable' : 'Enable'}</Text>
      </Button>
      <Dialog>{renderToggleDialog()}</Dialog>
    </DialogTrigger>
  );
};

export default ScriptToggleButton;
