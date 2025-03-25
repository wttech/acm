import { Button, ButtonGroup, Content, Dialog, DialogTrigger, Divider, Flex, Heading, Item, Switch, TabList, TabPanels, Tabs, Text } from '@adobe/react-spectrum';
import { ToastQueue } from '@react-spectrum/toast';
import Bug from '@spectrum-icons/workflow/Bug';
import Cancel from '@spectrum-icons/workflow/Cancel';
import Close from '@spectrum-icons/workflow/Close';
import FileCode from '@spectrum-icons/workflow/FileCode';
import Gears from '@spectrum-icons/workflow/Gears';
import Help from '@spectrum-icons/workflow/Help';
import Print from '@spectrum-icons/workflow/Print';
import Spellcheck from '@spectrum-icons/workflow/Spellcheck';
import { useCallback, useEffect, useState } from 'react';
import { useDebounce, useInterval } from 'react-use';
import CompilationStatus from '../components/CompilationStatus.tsx';
import ExecutionAbortButton from '../components/ExecutionAbortButton';
import ExecutionCopyOutputButton from '../components/ExecutionCopyOutputButton';
import ExecutionProgressBar from '../components/ExecutionProgressBar';
import ImmersiveEditor, { SyntaxError } from '../components/ImmersiveEditor';
import KeyboardShortcutsButton from '../components/KeyboardShortcutsButton';
import { apiRequest } from '../utils/api.ts';
import { Execution, ExecutionStatus, isExecutionPending, QueueOutput } from '../utils/api.types.ts';
import { StorageKeys } from '../utils/storage.ts';
import ConsoleCode from './ConsoleCode.groovy';
import CodeExecuteButton from "../components/CodeExecuteButton";

const toastTimeout = 3000;
const executionPollInterval = 500;
const compilationDelay = 1000;
type SelectedTab = 'code' | 'output';

const ConsolePage = () => {
  const [selectedTab, setSelectedTab] = useState<SelectedTab>('code');
  const [executing, setExecuting] = useState<boolean>(false);
  const [code, setCode] = useState<string | undefined>(localStorage.getItem(StorageKeys.EDITOR_CODE) || ConsoleCode);
  const [execution, setExecution] = useState<Execution | null>(null);
  const [compiling, setCompiling] = useState<boolean>(false);
  const [syntaxError, setSyntaxError] = useState<SyntaxError | undefined>(undefined);
  const [compileError, setCompileError] = useState<string | undefined>(undefined);
  const [autoscroll, setAutoscroll] = useState<boolean>(true);

  const compileCode = useCallback(async () => {
    try {
      const { data } = await apiRequest<Execution>({
        operation: 'Code parsing',
        url: `/apps/acm/api/execute-code.json`,
        method: 'post',
        data: {
          mode: 'parse',
          code: {
            id: 'console',
            content: code,
          },
        },
      });
      const queuedExecution = data.data;
      setExecution(queuedExecution);

      if (queuedExecution.error) {
        const [, lineText, columnText] = queuedExecution.error.match(/@ line (\d+), column (\d+)/) || [];

        if (!lineText || !columnText) {
          setCompileError(queuedExecution.error);
          return;
        }

        const line = parseInt(lineText, 10);
        const column = parseInt(columnText, 10);

        setSyntaxError({ line, column, message: queuedExecution.error });
      } else {
        setSyntaxError(undefined);
        setCompileError(undefined);
      }
    } catch {
      console.warn('Code parsing error!');
    } finally {
      setCompiling(false);
    }
  }, [code]);

  const [, cancelCompilation] = useDebounce(compileCode, compilationDelay, [code]);
  useDebounce(() => localStorage.setItem(StorageKeys.EDITOR_CODE, code || ''), compilationDelay, [code]);

  const onExecute = async () => {
    setExecuting(true);
    setExecution(null);

    // TODO do here '/apps/acm/api/describe-code.json' request

    try {
      const response = await apiRequest<QueueOutput>({
        operation: 'Code execution',
        url: `/apps/acm/api/queue-code.json`,
        method: 'post',
        data: {
          mode: 'run',
          code: {
            id: 'console',
            content: code,
          },
          arguments: {}, // TODO
        },
      });
      const queuedExecution = response.data.data.executions[0]!;
      setExecution(queuedExecution);
      setSelectedTab('output');
    } catch (error) {
      console.error('Code execution error:', error);
      setExecuting(false);
      ToastQueue.negative('Code execution error!', { timeout: toastTimeout });
    }
  };

  const pollExecutionState = async (jobId: string) => {
    try {
      const response = await apiRequest<QueueOutput>({
        operation: 'Code execution state',
        url: `/apps/acm/api/queue-code.json?jobId=${jobId}`,
        method: 'get',
      });
      const queuedExecution = response.data.data.executions.find((e: Execution) => e.id === jobId)!;
      setExecution(queuedExecution);

      if (!isExecutionPending(queuedExecution.status)) {
        setExecuting(false);
        if (queuedExecution.status === ExecutionStatus.FAILED) {
          ToastQueue.negative('Code execution failed!', {
            timeout: toastTimeout,
          });
        } else if (queuedExecution.status === ExecutionStatus.SKIPPED) {
          ToastQueue.neutral('Code execution cannot run!', {
            timeout: toastTimeout,
          });
        } else if (queuedExecution.status === ExecutionStatus.SUCCEEDED) {
          ToastQueue.positive('Code execution succeeded!', {
            timeout: toastTimeout,
          });
        }
      }
    } catch (error) {
      console.warn('Code execution state unknown:', error);
    }
  };

  useInterval(
    () => {
      if (execution && isExecutionPending(execution.status)) {
        pollExecutionState(execution.id);
      }
    },
    execution && isExecutionPending(execution.status) ? executionPollInterval : null,
  );

  useEffect(() => {
    setSyntaxError(undefined);
    setCompileError(undefined);
    setCompiling(true);

    return () => {
      cancelCompilation();
    };
  }, [cancelCompilation, code]);

  const executionOutput = ((execution?.output ?? '') + '\n' + (execution?.error ?? '')).trim();

  return (
    <Flex direction="column" flex="1" gap="size-200">
      <Tabs flex="1" aria-label="Code execution" selectedKey={selectedTab} onSelectionChange={(key) => setSelectedTab(key as SelectedTab)}>
        <TabList>
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
          <Item key="code">
            <Flex direction="column" gap="size-200" marginY="size-100" flex={1}>
              <Flex direction="row" justifyContent="space-between" alignItems="center">
                <Flex flex="1" alignItems="center">
                  <ButtonGroup>
                    <CodeExecuteButton code={code || ''} onExecute={onExecute} isPending={executing || compiling || !!syntaxError || !!compileError}/>
                  </ButtonGroup>
                </Flex>
                <Flex flex="1" justifyContent="center" alignItems="center">
                  <CompilationStatus onErrorClick={() => setSelectedTab('output')} compiling={compiling} syntaxError={syntaxError} compileError={compileError} />
                </Flex>
                <Flex flex="1" justifyContent="end" alignItems="center">
                  <KeyboardShortcutsButton />
                </Flex>
              </Flex>
              <ImmersiveEditor id="code-editor" initialValue={code} readOnly={executing} onChange={setCode} syntaxError={syntaxError} language="groovy" />
            </Flex>
          </Item>
          <Item key="output">
            <Flex direction="column" gap="size-200" marginY="size-100" flex={1}>
              <Flex direction="row" justifyContent="space-between" alignItems="center">
                <Flex flex="1" alignItems="center">
                  <ButtonGroup>
                    <ExecutionAbortButton execution={execution} onComplete={setExecution} />
                    <ExecutionCopyOutputButton output={executionOutput} />
                  </ButtonGroup>
                  <Switch isSelected={autoscroll} isDisabled={!isExecutionPending(execution?.status)} marginStart={20} onChange={() => setAutoscroll((prev) => !prev)}>
                    <Text>Autoscroll</Text>
                  </Switch>
                </Flex>
                <Flex flex="1" justifyContent="center" alignItems="center">
                  <ExecutionProgressBar execution={execution} active={executing} />
                </Flex>
                <Flex flex="1" justifyContent="end" alignItems="center">
                  <DialogTrigger>
                    <Button variant="secondary" style="fill">
                      <Help />
                      <Text>Help</Text>
                    </Button>
                    {(close) => (
                      <Dialog>
                        <Heading>Code execution</Heading>
                        <Divider />
                        <Content>
                          <p>
                            <Print size="XS" /> Output is printed live.
                          </p>
                          <p>
                            <Cancel size="XS" /> <Text>Abort if the execution:</Text>
                            <ul style={{ listStyleType: 'none' }}>
                              <li>
                                <Spellcheck size="XS" /> is taking too long
                              </li>
                              <li>
                                <Bug size="XS" /> is stuck in an infinite loop
                              </li>
                              <li>
                                <Gears size="XS" /> makes the instance unresponsive
                              </li>
                            </ul>
                          </p>
                          <p>
                            <Help size="XS" /> Be aware that aborting execution may leave data in an inconsistent state.
                          </p>
                        </Content>
                        <ButtonGroup>
                          <Button variant="secondary" onPress={close}>
                            <Close size="XS" />
                            <Text>Close</Text>
                          </Button>
                        </ButtonGroup>
                      </Dialog>
                    )}
                  </DialogTrigger>
                </Flex>
              </Flex>
              <ImmersiveEditor id="output-preview" value={executionOutput} readOnly scrollToBottomOnUpdate={autoscroll} />
            </Flex>
          </Item>
        </TabPanels>
      </Tabs>
    </Flex>
  );
};

export default ConsolePage;
