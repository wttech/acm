import { Button, ButtonGroup, Content, Dialog, DialogContainer, Divider, Form, Heading, Item, TabList, Tabs, Text, View } from '@adobe/react-spectrum';
import Alert from '@spectrum-icons/workflow/Alert';
import Checkmark from '@spectrum-icons/workflow/Checkmark';
import Close from '@spectrum-icons/workflow/Close';
import PlayCircle from '@spectrum-icons/workflow/PlayCircle';
import React, { useState } from 'react';
import { FormProvider, useForm } from 'react-hook-form';
import { toastRequest } from '../utils/api';
import { ExecutableIdConsole } from '../types/executable.ts';
import { ExecutionStatus } from '../types/execution.ts';
import { Input, InputGroupDefault, InputValue, InputValues } from '../types/input.ts';
import { Objects } from '../utils/objects';
import { ToastTimeoutLong } from '../utils/spectrum.ts';
import { Strings } from '../utils/strings';
import CodeInput from './CodeInput.tsx';
import { Description } from '../types/main.ts';

interface CodeExecuteButtonProps {
  code: string;
  onDescribeFailed: (description: Description) => void;
  onExecute: (description: Description, args: InputValues) => void;
  isDisabled: boolean;
  isPending: boolean;
}

const CodeExecuteButton: React.FC<CodeExecuteButtonProps> = ({ code, onDescribeFailed, onExecute, isDisabled, isPending }) => {
  const [description, setDescription] = useState<Description | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [described, setDescribed] = useState(false);
  const [selectedTab, setSelectedTab] = useState(0);
  const methods = useForm<InputValues>();

  const { formState } = methods;

  const fetchDescription = async () => {
    setDescribed(true);
    try {
      const response = await toastRequest<Description>({
        operation: 'Describe code',
        hideAfter: ToastTimeoutLong,
        positive: false,
        url: `/apps/acm/api/describe-code.json`,
        method: 'post',
        data: {
          code: {
            id: ExecutableIdConsole,
            content: code,
          },
        },
      });
      const description = response.data.data;

      if (description.execution.status === ExecutionStatus.SUCCEEDED) {
        setDescription(description);

        const inputsInitial = description.inputs ? Object.fromEntries(Object.entries(description.inputs).map(([key, arg]) => [key, arg.value])) : {};
        methods.reset(inputsInitial);

        const inputsRequired = Objects.isNotEmpty(description.inputs);
        if (inputsRequired) {
          setDialogOpen(true);
        } else {
          onExecute(description, {});
        }
      } else if (description.execution.status === ExecutionStatus.FAILED) {
        onDescribeFailed(description);
      } else {
        console.error('Code description has unexpected status:', description.execution.status);
      }
    } finally {
      setDescribed(false);
    }
  };

  const handleExecute = () => {
    if (description) {
      onExecute(description, methods.getValues());
    } else {
      fetchDescription();
    }
  };

  const handleCloseDialog = () => {
    setDialogOpen(false);
    setDescription(null);
  };

  const handleFormSubmit = async (data: InputValues) => {
    handleCloseDialog();
    onExecute(description!, data);
  };

  const descriptionInputs: Input<InputValue>[] = Object.values(description?.inputs || []);
  const groups = Array.from(new Set(descriptionInputs.map((arg) => arg.group)));
  const shouldRenderTabs = groups.length > 1 || (groups.length === 1 && groups[0] !== InputGroupDefault);
  const validationFailed = Object.keys(formState.errors).length > 0;
  const groupHasError = (group: string): boolean => descriptionInputs.filter((arg) => arg.group === group).some((arg) => !!formState.errors[arg.name]);

  return (
    <>
      <Button aria-label="Execute" variant="accent" onPress={handleExecute} isPending={isPending || described} isDisabled={isDisabled}>
        <PlayCircle />
        <Text>Execute</Text>
      </Button>
      <DialogContainer onDismiss={handleCloseDialog}>
        {dialogOpen && (
          <Dialog minWidth="40vw">
            <FormProvider {...methods}>
              <Heading>Provide Inputs</Heading>
              <Divider />
              <Content>
                <Form onSubmit={methods.handleSubmit(handleFormSubmit)}>
                  {shouldRenderTabs ? (
                    <>
                      <Tabs aria-label="Input Groups" selectedKey={selectedTab} onSelectionChange={(key) => setSelectedTab(Number(key))}>
                        <TabList>
                          {groups.map((group, idx) => (
                            <Item key={idx} textValue={Strings.capitalize(group)}>
                              {groupHasError(group) && <Alert color="negative" />}
                              <Text>{Strings.capitalize(group)}</Text>
                            </Item>
                          ))}
                        </TabList>
                      </Tabs>
                      {groups.map((group, idx) => (
                        <View key={group} marginY="size-200" UNSAFE_style={{ display: selectedTab === idx ? 'block' : 'none' }}>
                          {descriptionInputs
                            .filter((arg) => arg.group === group)
                            .map((arg) => (
                              <CodeInput key={arg.name} input={arg} value={methods.getValues(arg.name)} onChange={(name, value) => methods.setValue(name, value)} />
                            ))}
                        </View>
                      ))}
                    </>
                  ) : (
                    descriptionInputs.map((arg) => <CodeInput key={arg.name} input={arg} value={methods.getValues(arg.name)} onChange={(name, value) => methods.setValue(name, value)} />)
                  )}
                </Form>
              </Content>
              <ButtonGroup>
                <Button aria-label="Cancel" variant="secondary" onPress={handleCloseDialog}>
                  <Close size="XS" />
                  <Text>Cancel</Text>
                </Button>
                <Button aria-label="Start" variant="cta" isDisabled={validationFailed} onPress={() => methods.handleSubmit(handleFormSubmit)()}>
                  <Checkmark size="XS" />
                  <Text>Start</Text>
                </Button>
              </ButtonGroup>
            </FormProvider>
          </Dialog>
        )}
      </DialogContainer>
    </>
  );
};

export default CodeExecuteButton;
