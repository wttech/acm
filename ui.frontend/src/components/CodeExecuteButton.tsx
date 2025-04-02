import { Button, ButtonGroup, Content, Dialog, DialogContainer, Divider, Heading, Item, TabList, TabPanels, Tabs, Text, View } from '@adobe/react-spectrum';
import Checkmark from '@spectrum-icons/workflow/Checkmark';
import Close from '@spectrum-icons/workflow/Close';
import Gears from '@spectrum-icons/workflow/Gears';
import React, { useState } from 'react';
import { toastRequest } from '../utils/api.ts';
import { Argument, ArgumentGroupDefault, ArgumentValue, ArgumentValues, Description, ExecutableIdConsole, ExecutionStatus } from '../utils/api.types.ts';
import { Strings } from '../utils/strings.ts';
import CodeArgumentInput from './CodeArgumentInput.tsx';

interface CodeExecuteButtonProps {
  code: string;
  onDescribeFailed: (description: Description) => void;
  onExecute: (description: Description, args: ArgumentValues) => void;
  isDisabled: boolean;
  isPending: boolean;
}

const CodeExecuteButton: React.FC<CodeExecuteButtonProps> = ({ code, onDescribeFailed, onExecute, isDisabled, isPending }) => {
  const [description, setDescription] = useState<Description | null>(null);
  const [args, setArgs] = useState<ArgumentValues>({});
  const [dialogOpen, setDialogOpen] = useState(false);
  const [described, setDescribed] = useState(false);

  const fetchDescription = async () => {
    setDescribed(true);
    try {
      const response = await toastRequest<Description>({
        operation: 'Describe code',
        timeout: 10000,
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

        const argumentsInitial = description.arguments ? Object.fromEntries(Object.entries(description.arguments).map(([key, arg]) => [key, arg.value])) : {};
        setArgs(argumentsInitial);

        const argumentsRequired = description.arguments && Object.keys(description.arguments).length > 0;
        if (argumentsRequired) {
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
      onExecute(description, args);
    } else {
      fetchDescription();
    }
  };

  const handleCloseDialog = () => {
    setDialogOpen(false);
    setDescription(null);
  };

  const handleArgumentChange = (name: string, value: ArgumentValue) => {
    setArgs({ ...args, [name]: value });
  };

  const descriptionArguments: Argument<ArgumentValue>[] = Object.values(description?.arguments || []);
  const groups = Array.from(new Set(descriptionArguments.map((arg) => arg.group)));
  const shouldRenderTabs = groups.length > 1 || (groups.length === 1 && groups[0] !== ArgumentGroupDefault);

  return (
    <>
      <Button variant="accent" onPress={handleExecute} isPending={isPending || described} isDisabled={isDisabled}>
        <Gears />
        <Text>Execute</Text>
      </Button>
      <DialogContainer onDismiss={handleCloseDialog}>
        {dialogOpen && (
          <Dialog>
            <Heading>Provide Arguments</Heading>
            <Divider />
            <Content>
              {shouldRenderTabs ? (
                <Tabs>
                  <TabList>
                    {groups.map((group) => (
                      <Item key={group}>{Strings.capitalize(group)}</Item>
                    ))}
                  </TabList>
                  <TabPanels>
                    {groups.map((group) => (
                      <Item key={group}>
                        <View marginY="size-200">
                          {descriptionArguments
                            .filter((arg) => arg.group === group)
                            .map((arg) => (
                              <CodeArgumentInput key={arg.name} arg={arg} value={args[arg.name]} onChange={handleArgumentChange} />
                            ))}
                        </View>
                      </Item>
                    ))}
                  </TabPanels>
                </Tabs>
              ) : (
                descriptionArguments.map((arg) => <CodeArgumentInput key={arg.name} arg={arg} value={args[arg.name]} onChange={handleArgumentChange} />)
              )}
            </Content>
            <ButtonGroup>
              <Button variant="secondary" onPress={handleCloseDialog}>
                <Close size="XS" />
                <Text>Cancel</Text>
              </Button>
              <Button
                variant="cta"
                onPress={() => {
                  handleCloseDialog();
                  onExecute(description!, args);
                }}
              >
                <Checkmark size="XS" />
                <Text>Start</Text>
              </Button>
            </ButtonGroup>
          </Dialog>
        )}
      </DialogContainer>
    </>
  );
};

export default CodeExecuteButton;
