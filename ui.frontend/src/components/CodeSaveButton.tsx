import { Button, ButtonGroup, Content, Dialog, DialogContainer, Divider, Form, Heading, Radio, RadioGroup, Text, TextField } from '@adobe/react-spectrum';
import { ToastQueue } from '@react-spectrum/toast';
import Checkmark from '@spectrum-icons/workflow/Checkmark';
import Close from '@spectrum-icons/workflow/Close';
import FileAdd from '@spectrum-icons/workflow/FileAdd';
import Hand from '@spectrum-icons/workflow/Hand';
import Launch from '@spectrum-icons/workflow/Launch';
import React, { useState } from 'react';
import { toastRequest } from '../utils/api';
import { ScriptRoots } from '../utils/api.types';

type ScriptType = 'manual' | 'automatic';

interface CodeSaveButtonProps extends React.ComponentProps<typeof Button> {
  code: string;
}

const CodeSaveButton: React.FC<CodeSaveButtonProps> = ({ code, ...buttonProps }) => {
  const [dialogOpen, setDialogOpen] = useState(false);
  const [scriptType, setScriptType] = useState<ScriptType>('manual');
  const [scriptName, setScriptName] = useState('');
  const [saving, setSaving] = useState(false);

  const id = ScriptRoots[scriptType] + '/' + (scriptName.trim() || '<name>');

  const handleOpen = () => setDialogOpen(true);
  const handleClose = () => {
    setDialogOpen(false);
    setScriptName('');
    setSaving(false);
  };

  const handleSave = async () => {
    if (!scriptName.trim()) {
      ToastQueue.negative('Script name is required');
      return;
    }
    setSaving(true);
    const path = ScriptRoots[scriptType] + scriptName.trim();
    await toastRequest({
      operation: 'Save script',
      url: '/apps/acm/api/script.json',
      method: 'POST',
      data: {
        id: path,
        content: code,
      },
    });
    ToastQueue.positive('Script saved successfully');
    handleClose();
  };

  return (
    <>
      <Button onPress={handleOpen} {...buttonProps}>
        <FileAdd />
        <Text>Save</Text>
      </Button>
      <DialogContainer onDismiss={handleClose}>
        {dialogOpen && (
          <Dialog minWidth="40vw">
            <Heading>Save Script</Heading>
            <Divider />
            <Content>
              <Form validationBehavior="native">
                <RadioGroup label="Type" isRequired value={scriptType} onChange={(value) => setScriptType(value as ScriptType)} orientation="horizontal">
                  <Radio value="manual">
                    <Hand size="XS" />
                    <Text marginStart="size-50">Manual</Text>
                  </Radio>
                  <Radio value="automatic">
                    <Launch size="XS" />
                    <Text marginStart="size-50">Automatic</Text>
                  </Radio>
                </RadioGroup>
                <TextField label="Name" width="100%" value={scriptName} onChange={setScriptName} isRequired marginTop="size-200" />
                <TextField label="ID" width="100%" value={id} isDisabled marginTop="size-200" />
              </Form>
            </Content>
            <ButtonGroup>
              <Button variant="secondary" onPress={handleClose}>
                <Close size="XS" />
                <Text>Cancel</Text>
              </Button>
              <Button variant="cta" onPress={handleSave} isPending={saving}>
                <Checkmark size="XS" />
                <Text>Save</Text>
              </Button>
            </ButtonGroup>
          </Dialog>
        )}
      </DialogContainer>
    </>
  );
};

export default CodeSaveButton;
