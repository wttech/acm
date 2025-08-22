import { Badge, Button, ButtonGroup, Content, Flex, IllustratedMessage, Item, LabeledValue, ProgressBar, Switch, TabList, TabPanels, Tabs, Text, View } from '@adobe/react-spectrum';
import { Editor } from '@monaco-editor/react';
import { Field } from '@react-spectrum/label';
import { ToastQueue } from '@react-spectrum/toast';
import NotFound from '@spectrum-icons/illustrations/NotFound';
import Copy from '@spectrum-icons/workflow/Copy';
import FileCode from '@spectrum-icons/workflow/FileCode';
import History from '@spectrum-icons/workflow/History';
import InfoOutline from '@spectrum-icons/workflow/InfoOutline';
import Print from '@spectrum-icons/workflow/Print';
import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import CodeEditor from '../components/CodeEditor.tsx';
import ExecutableIdValue from '../components/ExecutableIdValue';
import ExecutionAbortButton from '../components/ExecutionAbortButton';
import ExecutionCopyOutputButton from '../components/ExecutionCopyOutputButton';
import ExecutionProgressBar from '../components/ExecutionProgressBar';
import ExecutionStatusBadge from '../components/ExecutionStatusBadge';
import { useAppState } from '../hooks/app.ts';
import { useExecutionPolling } from '../hooks/execution';
import { useFormatter } from '../hooks/formatter';
import { useNavigationTab } from '../hooks/navigation';
import { isExecutableScript, isExecutionPending } from '../utils/api.types';
import { GROOVY_LANGUAGE_ID } from '../utils/monaco/groovy.ts';
import { LOG_LANGUAGE_ID } from '../utils/monaco/log.ts';
import { Objects } from '../utils/objects';
import { ToastTimeoutQuick } from '../utils/spectrum.ts';

const ExecutionView = () => {
  const appState = useAppState();
  const { executionId } = useParams<{ executionId: string }>();
  const formatter = useFormatter();
  const [autoscrollOutput, setAutoscrollOutput] = useState<boolean>(true);
  const { execution, setExecution, loading } = useExecutionPolling(executionId, appState.spaSettings.executionPollInterval);
  const [selectedTab, handleTabChange] = useNavigationTab('details');
  const navigate = useNavigate();

  if (loading) {
    return (
      <Flex flex="1" justifyContent="center" alignItems="center">
        <ProgressBar label="Loading..." isIndeterminate />
      </Flex>
    );
  }

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

  const onCopyExecutableCode = () => {
    navigator.clipboard
      .writeText(execution.executable.content)
      .then(() => {
        ToastQueue.info('Execution code copied to clipboard!', { timeout: ToastTimeoutQuick });
      })
      .catch(() => {
        ToastQueue.negative('Failed to copy execution code!', { timeout: ToastTimeoutQuick });
      });
  };

  return (
    <Flex direction="column" flex="1" gap="size-400">
      <Tabs flex="1" aria-label="Executions" selectedKey={selectedTab} onSelectionChange={handleTabChange}>
        <TabList>
          <Item key="details" aria-label="Details">
            <History />
            <Text>Execution</Text>
          </Item>
          <Item key="code" aria-label="Code">
            <FileCode />
            <Text>Code</Text>
          </Item>
          <Item key="output" aria-label="Output">
            <Print />
            <Text>Output</Text>
          </Item>
        </TabList>
        <TabPanels flex="1" UNSAFE_style={{ display: 'flex' }}>
          <Item key="details" aria-label="Details">
            <Flex direction="column" flex="1" gap="size-200" marginY="size-100">
              <View backgroundColor="gray-50" padding="size-200" borderRadius="medium" borderColor="dark" borderWidth="thin">
                <Flex direction="row" justifyContent="space-between" gap="size-200">
                  <LabeledValue label="ID" value={execution.id} flex="1" />
                  <LabeledValue label="User" value={execution.userId} flex="1" />
                  <Flex justifyContent="end">
                    <Field label="Status" flex="1">
                      <div>
                        <ExecutionStatusBadge value={execution.status} />
                      </div>
                    </Field>
                  </Flex>
                </Flex>
              </View>
              <View backgroundColor="gray-50" padding="size-200" borderRadius="medium" borderColor="dark" borderWidth="thin">
                <Flex direction="row" justifyContent="space-between" gap="size-200">
                  <LabeledValue label="Started At" value={execution.startDate ? formatter.dateExplained(execution.startDate) : '—'} />
                  <LabeledValue label="Duration" value={formatter.durationExplained(execution.duration)} />
                  <LabeledValue label="Ended At" value={execution.endDate ? formatter.dateExplained(execution.endDate) : '—'} />
                </Flex>
              </View>
              <View backgroundColor="gray-50" padding="size-200" borderRadius="medium" borderColor="dark" borderWidth="thin">
                <Flex direction="row" justifyContent="space-between" gap="size-200">
                  <Field label="Executable" flex="1" minWidth="50%">
                    <div>
                      <ExecutableIdValue id={execution.executable.id} />
                    </div>
                  </Field>
                  <Field label="Arguments" flex="1" minWidth="50%">
                    <div>
                      {Objects.isEmpty(execution.executable.arguments) ? (
                        <Badge variant="neutral">
                          <InfoOutline />
                          <Text>Not described</Text>
                        </Badge>
                      ) : (
                        <View width="100%" backgroundColor="gray-800" borderWidth="thin" position="relative" borderColor="dark" height="100%" borderRadius="medium" padding="size-50">
                          <Editor width="100%" language="json" theme="vs-dark" height="200px" options={{ readOnly: true, scrollBeyondLastLine: false }} value={JSON.stringify(execution.executable.arguments, null, 2)} />
                        </View>
                      )}
                    </div>
                  </Field>
                </Flex>
              </View>
            </Flex>
          </Item>
          <Item key="code" aria-label="Code">
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
              <CodeEditor id="execution-view" value={execution.executable.content} language={GROOVY_LANGUAGE_ID} readOnly />
            </Flex>
          </Item>
          <Item key="output" aria-label="Output">
            <Flex direction="column" flex="1" gap="size-200" marginY="size-100">
              <Flex direction="row" justifyContent="space-between" alignItems="center">
                <Flex flex="1" alignItems="center">
                  <ButtonGroup>
                    <ExecutionAbortButton execution={execution} onComplete={setExecution} />
                    <ExecutionCopyOutputButton output={executionOutput} />
                    {isExecutableScript(execution.executable.id) && (
                      <Button variant="secondary" style="fill" onPress={() => navigate(`/scripts/view/${encodeURIComponent(execution?.executable.id)}`)}>
                        <FileCode />
                        <Text>View in scripts</Text>
                      </Button>
                    )}
                  </ButtonGroup>
                </Flex>
                <Flex flex="1" justifyContent="center" alignItems="center">
                  <ExecutionProgressBar execution={execution} />
                </Flex>
                <Flex flex="1" justifyContent="end" alignItems="center">
                  <Switch isSelected={autoscrollOutput} isDisabled={!isExecutionPending(execution.status)} marginStart={20} onChange={() => setAutoscrollOutput((prev) => !prev)}>
                    <Text>Autoscroll</Text>
                  </Switch>
                </Flex>
              </Flex>
              <CodeEditor id="execution-output" value={executionOutput} readOnly scrollToBottomOnUpdate={autoscrollOutput} language={LOG_LANGUAGE_ID} />
            </Flex>
          </Item>
        </TabPanels>
      </Tabs>
    </Flex>
  );
};

export default ExecutionView;
