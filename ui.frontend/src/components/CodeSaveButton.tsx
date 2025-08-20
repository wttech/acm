import { Button, ButtonGroup, Content, Dialog, DialogContainer, Divider, Form, Heading, Radio, RadioGroup, Text, TextField } from '@adobe/react-spectrum';
import { ToastQueue } from '@react-spectrum/toast';
import Checkmark from '@spectrum-icons/workflow/Checkmark';
import Close from '@spectrum-icons/workflow/Close';
import FileAdd from '@spectrum-icons/workflow/FileAdd';
import Hand from '@spectrum-icons/workflow/Hand';
import Launch from '@spectrum-icons/workflow/Launch';
import React, { useState } from 'react';
import { toastRequest } from '../utils/api';
import { ScriptRoots, ScriptType } from '../utils/api.types';
import { Strings } from '../utils/strings';

interface CodeSaveButtonProps extends React.ComponentProps<typeof Button> {
  code: string;
}

function detectScriptType(code: string): ScriptType {
  const automaticPattern = /(def|Schedule)\s+scheduleRun\s*(\(\s*\))?\s*\{/;
  return automaticPattern.test(code) ? ScriptType.AUTOMATIC : ScriptType.MANUAL;
}

// TODO add warning that automatic script could be immediately executed on author after saving; and on publishes after clicking synchronize (checkbox in dialog)
const CodeSaveButton: React.FC<CodeSaveButtonProps> = ({ code, ...buttonProps }) => {
  const [dialogOpen, setDialogOpen] = useState(false);
  const [scriptType, setScriptType] = useState<ScriptType>(detectScriptType(code));
  const [scriptName, setScriptName] = useState('');
  const [saving, setSaving] = useState(false);

  const scriptNameValid = Strings.checkFilePath(scriptName);
  const scriptId = ScriptRoots[scriptType] + '/' + (Strings.removeEnd(scriptName.trim(), '.groovy') || '{name}') + '.groovy';

  const handleOpen = () => {
    setScriptType(detectScriptType(code));
    setDialogOpen(true);
  };

  const handleClose = () => {
    setDialogOpen(false);
    setScriptName('');
    setSaving(false);
  };

  const handleSave = async () => {
    if (!scriptName.trim()) {
      ToastQueue.negative('Script name is required!');
      return;
    }
    if (!scriptNameValid) {
      ToastQueue.negative('Script name is invalid!');
      return;
    }

    setSaving(true);
    try {
      await toastRequest({
        operation: 'Save script',
        url: '/apps/acm/api/script.json?action=save',
        method: 'POST',
        data: {
          code: {
            id: scriptId,
            content: code,
          },
        },
      });
      handleClose();
    } finally {
      setSaving(false);
    }
  };

  const handleFormSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await handleSave();
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
              <Form validationBehavior="native" onSubmit={handleFormSubmit}>
                <RadioGroup label="Type" isRequired value={scriptType} onChange={(value) => setScriptType(value as ScriptType)} orientation="horizontal">
                  <Radio value={ScriptType.MANUAL}>
                    <Hand size="XS" />
                    <Text marginStart="size-50">Manual</Text>
                  </Radio>
                  <Radio value={ScriptType.AUTOMATIC}>
                    <Launch size="XS" />
                    <Text marginStart="size-50">Automatic</Text>
                  </Radio>
                </RadioGroup>
                <TextField
                  label="Name"
                  width="100%"
                  value={scriptName}
                  onChange={setScriptName}
                  isRequired
                  marginTop="size-200"
                  validationState={scriptName && !scriptNameValid ? 'invalid' : undefined}
                  errorMessage={scriptName && !scriptNameValid ? 'Invalid format' : undefined}
                />
                <TextField label="ID" width="100%" value={scriptId} isDisabled marginTop="size-200" />
              </Form>
            </Content>
            <ButtonGroup>
              <Button variant="secondary" onPress={handleClose}>
                <Close size="XS" />
                <Text>Cancel</Text>
              </Button>
              <Button variant="cta" type="submit" isPending={saving} isDisabled={!scriptNameValid || saving}>
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
