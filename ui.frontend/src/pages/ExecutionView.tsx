import { Button, ButtonGroup, Content, Flex, IllustratedMessage, Item, LabeledValue, TabList, TabPanels, Tabs, Text, View } from '@adobe/react-spectrum';
import { Field } from '@react-spectrum/label';
import { ToastQueue } from '@react-spectrum/toast';
import NotFound from '@spectrum-icons/illustrations/NotFound';
import Copy from '@spectrum-icons/workflow/Copy';
import FileCode from '@spectrum-icons/workflow/FileCode';
import History from '@spectrum-icons/workflow/History';
import Print from '@spectrum-icons/workflow/Print';
import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import ExecutableValue from '../components/ExecutableValue.tsx';
import ExecutionStatusBadge from '../components/ExecutionStatusBadge.tsx';
import ImmersiveEditor from '../components/ImmersiveEditor.tsx';
import { toastRequest } from '../utils/api';
import { Execution, ExecutionOutput } from '../utils/api.types';
import { useFormatter } from '../utils/hooks/formatter';

const toastTimeout = 3000;

const ExecutionView = () => {
  const [execution, setExecution] = useState<Execution | null>(null);
  const executionId = decodeURIComponent(useParams<{ executionId: string }>().executionId as string);
  const formatter = useFormatter();

  useEffect(() => {
    const fetchExecution = async () => {
      try {
        const response = await toastRequest<ExecutionOutput>({
          method: 'GET',
          url: `/apps/contentor/api/execution.json?id=${executionId}`,
          operation: `Execution loading`,
          positive: false,
        });
        setExecution(response.data.data.list[0]);
      } catch (error) {
        console.error(`Execution cannot be loaded '${executionId}':`, error);
      }
    };
    fetchExecution();
  }, [executionId]);

  if (!execution) {
    return (
      <Flex direction="column" flex="1">
        <IllustratedMessage>
          <NotFound />
          <Content>Execution not found</Content>
        </IllustratedMessage>
      </Flex>
    );
  }

  const executionOutput = ((execution.output ?? '') + '\n' + (execution.error ?? '')).trim();

  const onCopyExecutionOutput = () => {
    if (executionOutput) {
      navigator.clipboard
        .writeText(executionOutput)
        .then(() => {
          ToastQueue.info('Execution output copied to clipboard!', {
            timeout: toastTimeout,
          });
        })
        .catch(() => {
          ToastQueue.negative('Failed to copy execution output!', {
            timeout: toastTimeout,
          });
        });
    } else {
      ToastQueue.negative('No execution output to copy!', {
        timeout: toastTimeout,
      });
    }
  };

  const onCopyExecutableCode = () => {
    navigator.clipboard
      .writeText(execution.executable.content)
      .then(() => {
        ToastQueue.info('Execution code copied to clipboard!', {
          timeout: toastTimeout,
        });
      })
      .catch(() => {
        ToastQueue.negative('Failed to copy execution code!', {
          timeout: toastTimeout,
        });
      });
  };

  return (
    <Flex direction="column" flex="1" gap="size-400">
      <Tabs flex="1" aria-label="Executions">
        <TabList>
          <Item key="details">
            <History />
            <Text>Execution</Text>
          </Item>
          <Item key="code" aria-label="Code">
            <FileCode />
            <Text>Code</Text>
          </Item>
          <Item key="output" aria-label="Execution">
            <Print />
            <Text>Output</Text>
          </Item>
        </TabList>
        <TabPanels flex="1" UNSAFE_style={{ display: 'flex' }}>
          <Item key="details">
            <Flex direction="column" flex="1" gap="size-200" marginY="size-100">
              <View backgroundColor="gray-50" padding="size-200" borderRadius="medium" borderColor="dark" borderWidth="thin">
                <Flex direction="row" justifyContent="space-between" gap="size-200">
                  <LabeledValue label="ID" value={execution.id} />
                  <Field label="Status">
                    <div>
                      <ExecutionStatusBadge value={execution.status} />
                    </div>
                  </Field>
                </Flex>
              </View>
              <View backgroundColor="gray-50" padding="size-200" borderRadius="medium" borderColor="dark" borderWidth="thin">
                <Field label="Executable" width="100%">
                  <div>
                    <ExecutableValue value={execution.executable} />
                  </div>
                </Field>
              </View>
              <View backgroundColor="gray-50" padding="size-200" borderRadius="medium" borderColor="dark" borderWidth="thin">
                <Flex direction="row" justifyContent="space-between" gap="size-200">
                  <LabeledValue label="Started At" value={formatter.dateExplained(execution.startDate)} />
                  <LabeledValue label="Duration" value={formatter.durationExplained(execution.duration)} />
                  <LabeledValue label="Ended At" value={formatter.dateExplained(execution.endDate)} />
                </Flex>
              </View>
            </Flex>
          </Item>
          <Item key="code">
            <Flex direction="column" flex="1" gap="size-200" marginY="size-100">
              <View>
                <Flex justifyContent="space-between" alignItems="center">
                  <ButtonGroup>
                    <Button variant="secondary" isDisabled={!executionOutput} onPress={onCopyExecutableCode}>
                      <Copy />
                      <Text>Copy</Text>
                    </Button>
                  </ButtonGroup>
                </Flex>
              </View>
              <ImmersiveEditor id="execution-view" value={execution.executable.content} language="groovy" readOnly />
            </Flex>
          </Item>
          <Item key="output">
            <Flex direction="column" flex="1" gap="size-200" marginY="size-100">
              <View>
                <Flex justifyContent="space-between" alignItems="center">
                  <ButtonGroup>
                    <Button variant="secondary" isDisabled={!executionOutput} onPress={onCopyExecutionOutput}>
                      <Copy />
                      <Text>Copy</Text>
                    </Button>
                  </ButtonGroup>
                </Flex>
              </View>
              <ImmersiveEditor id="execution-output" value={executionOutput} readOnly />
            </Flex>
          </Item>
        </TabPanels>
      </Tabs>
    </Flex>
  );
};

export default ExecutionView;
