import { Button, ButtonGroup, Checkbox, Content, Dialog, DialogContainer, Divider, Form, Heading, InlineAlert, Radio, RadioGroup, Text, TextField } from '@adobe/react-spectrum';
import Checkmark from '@spectrum-icons/workflow/Checkmark';
import Close from '@spectrum-icons/workflow/Close';
import FileAdd from '@spectrum-icons/workflow/FileAdd';
import Hand from '@spectrum-icons/workflow/Hand';
import Launch from '@spectrum-icons/workflow/Launch';
import UploadToCloud from '@spectrum-icons/workflow/UploadToCloud';
import React, { useState } from 'react';
import { Controller, FormProvider, useForm } from 'react-hook-form';
import { ScriptRoots, ScriptType } from '../types/script';
import { toastRequest } from '../utils/api';
import { Strings } from '../utils/strings';

interface CodeSaveButtonProps extends React.ComponentProps<typeof Button> {
  code: string;
}

interface CodeFormValues {
  scriptName: string;
  scriptType: ScriptType;
  sync: boolean;
}

function getFormDefaults(code: string): CodeFormValues {
  return {
    scriptName: '',
    scriptType: detectScriptType(code),
    sync: true,
  };
}

function detectScriptType(code: string): ScriptType {
  const automaticPattern = /(def|Schedule)\s+scheduleRun\s*(\(\s*\))?\s*\{/;
  return automaticPattern.test(code) ? ScriptType.AUTOMATIC : ScriptType.MANUAL;
}

const CodeSaveButton: React.FC<CodeSaveButtonProps> = ({ code, ...buttonProps }) => {
  const [dialogOpen, setDialogOpen] = useState(false);
  const [saving, setSaving] = useState(false);

  const methods = useForm<CodeFormValues>({ defaultValues: getFormDefaults(code) });
  const { control, handleSubmit, formState, reset, watch } = methods;
  const scriptName = watch('scriptName');
  const scriptType = watch('scriptType');
  const sync = watch('sync');
  const scriptId = ScriptRoots[scriptType] + '/' + (Strings.removeEnd(scriptName?.trim(), '.groovy') || '{name}') + '.groovy';

  const handleOpen = () => {
    reset(getFormDefaults(code));
    setDialogOpen(true);
  };

  const handleClose = () => {
    setDialogOpen(false);
    setSaving(false);
    reset(getFormDefaults(code));
  };

  const onSubmit = async (data: CodeFormValues) => {
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
      if (scriptType === ScriptType.AUTOMATIC && data.sync) {
        await toastRequest({
          method: 'POST',
          url: `/apps/acm/api/script.json?action=sync`,
          operation: `Synchronize scripts`,
        });
      }
      handleClose();
    } finally {
      setSaving(false);
    }
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
            <FormProvider {...methods}>
              <Heading>Save Script</Heading>
              <Divider />
              <Content>
                <Form onSubmit={handleSubmit(onSubmit)}>
                  <Controller
                    name="scriptType"
                    control={control}
                    render={({ field }) => (
                      <RadioGroup {...field} label="Type" isRequired value={field.value} onChange={field.onChange} orientation="horizontal">
                        <Radio value={ScriptType.MANUAL}>
                          <Hand size="XS" />
                          <Text marginStart="size-50">Manual</Text>
                        </Radio>
                        <Radio value={ScriptType.AUTOMATIC}>
                          <Launch size="XS" />
                          <Text marginStart="size-50">Automatic</Text>
                        </Radio>
                      </RadioGroup>
                    )}
                  />
                  <Controller
                    name="scriptName"
                    control={control}
                    rules={{
                      required: 'Value is required',
                      validate: (value) => Strings.checkFilePath(value) || 'Value has invalid format',
                    }}
                    render={({ field }) => (
                      <TextField {...field} label="Name" width="100%" isRequired marginTop="size-200" validationState={formState.errors.scriptName ? 'invalid' : undefined} errorMessage={formState.errors.scriptName?.message} />
                    )}
                  />
                  <TextField label="ID" width="100%" value={scriptId} isDisabled marginTop="size-200" />
                  <Controller
                    name="sync"
                    control={control}
                    render={({ field: { value, onChange, onBlur, name, ref } }) => (
                      <Checkbox isHidden={scriptType === ScriptType.MANUAL} isSelected={value} onChange={onChange} onBlur={onBlur} name={name} ref={ref} marginTop="size-200">
                        <UploadToCloud size="XS" />
                        <Text marginStart="size-50">Synchronize</Text>
                      </Checkbox>
                    )}
                  />
                </Form>
                {scriptType === ScriptType.AUTOMATIC && (
                  <InlineAlert width="100%" variant="notice" marginTop="size-100" UNSAFE_style={{ padding: '8px' }}>
                    <Heading>Warning</Heading>
                    <Content UNSAFE_style={{ padding: '6px', marginTop: '6px' }}>
                      <Text>{sync ? 'This action may cause immediate execution on the author and publish instances.' : 'This action may cause immediate execution on the author instance.'}</Text>
                    </Content>
                  </InlineAlert>
                )}
              </Content>
              <ButtonGroup>
                <Button variant="secondary" onPress={handleClose}>
                  <Close size="XS" />
                  <Text>Cancel</Text>
                </Button>
                <Button variant="cta" isPending={saving} isDisabled={saving} onPress={() => handleSubmit(onSubmit)()} type="button">
                  <Checkmark size="XS" />
                  <Text>Save</Text>
                </Button>
              </ButtonGroup>
            </FormProvider>
          </Dialog>
        )}
      </DialogContainer>
    </>
  );
};

export default CodeSaveButton;
