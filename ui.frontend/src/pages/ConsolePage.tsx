import { Button, ButtonGroup, Content, Dialog, DialogTrigger, Divider, Flex, Heading, Item, Keyboard, TabList, TabPanels, Tabs, Text, View } from '@adobe/react-spectrum';
import Bug from '@spectrum-icons/workflow/Bug';
import Cancel from '@spectrum-icons/workflow/Cancel';
import Copy from '@spectrum-icons/workflow/Copy';
import FileCode from '@spectrum-icons/workflow/FileCode';
import Gears from '@spectrum-icons/workflow/Gears';
import Help from '@spectrum-icons/workflow/Help';
import Print from '@spectrum-icons/workflow/Print';
import Spellcheck from '@spectrum-icons/workflow/Spellcheck';
import Close from "@spectrum-icons/workflow/Close";
import { useDebounce } from 'react-use';
import CompilationStatus from '../components/CompilationStatus.tsx';
import ImmersiveEditor, { SyntaxError } from '../components/ImmersiveEditor.tsx';
import { StorageKeys } from '../utils/storage.ts';
import ConsoleCode from './ConsoleCode.groovy';
import { ToastQueue } from '@react-spectrum/toast';
import React, { useCallback, useEffect, useRef, useState } from 'react';
import ExecutionProgressBar from '../components/ExecutionProgressBar';
import { apiRequest } from '../utils/api.ts';
import {Execution, ExecutionStatus, isExecutionPending, QueueOutput} from '../utils/api.types.ts';
import { registerGroovyLanguage } from '../utils/monaco/groovy.ts';

const toastTimeout = 3000;
const executionPollDelay = 500;
const executionPollInterval = 500;
const compileDelay = 1000;
type SelectedTab = 'code' | 'output';

const ConsolePage = () => {
  const [selectedTab, setSelectedTab] = useState<SelectedTab>('code');
  const [executing, setExecuting] = useState<boolean>(false);
  const [code, setCode] = useState<string | undefined>(localStorage.getItem(StorageKeys.EDITOR_CODE) || ConsoleCode);
  const [execution, setExecution] = useState<Execution | null>(null);
  const pollExecutionRef = useRef<number | null>(null);
  const [isCompiling, setIsCompiling] = useState<boolean>(false);
  const [syntaxError, setSyntaxError] = useState<SyntaxError | undefined>(undefined);
  const [compilationError, setCompilationError] = useState<string | undefined>(undefined);

  const compileCode = useCallback(async () => {
    localStorage.setItem(StorageKeys.EDITOR_CODE, code || '');

    try {
      const { data } = await apiRequest<Execution>({
        operation: 'Code parsing',
        url: `/apps/contentor/api/execute-code.json`,
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
          setCompilationError(queuedExecution.error);
          return;
        }

        const line = parseInt(lineText, 10);
        const column = parseInt(columnText, 10);

        setSyntaxError({ line, column, message: queuedExecution.error });
      } else {
        setSyntaxError(undefined);
        setCompilationError(undefined);
      }
    } catch {
      console.warn('Code parsing error!');
    } finally {
      setIsCompiling(false);
    }
  }, [code]);

  const [, cancelCompilation] = useDebounce(compileCode, compileDelay, [code]);

  const onExecute = async () => {
    setExecuting(true);
    setExecution(null);

    try {
      const response = await apiRequest<QueueOutput>({
        operation: 'Code execution',
        url: `/apps/contentor/api/queue-code.json`,
        method: 'post',
        data: {
          mode: 'evaluate',
          code: {
            id: 'console',
            content: code,
          },
        },
      });
      const queuedExecution = response.data.data.executions[0]!;
      setExecution(queuedExecution);
      setSelectedTab('output');

      window.setTimeout(() => {
        pollExecutionRef.current = window.setInterval(() => {
          pollExecutionState(queuedExecution.id);
        }, executionPollInterval);
      }, executionPollDelay);
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
        url: `/apps/contentor/api/queue-code.json?jobId=${jobId}`,
        method: 'get',
      });
      const queuedExecution = response.data.data.executions.find((e: Execution) => e.id === jobId)!;
      setExecution(queuedExecution);

      if (!isExecutionPending(queuedExecution.status)) {
        clearInterval(pollExecutionRef.current!);
        setExecuting(false);
        if (queuedExecution.status === ExecutionStatus.FAILED) {
          ToastQueue.negative('Code execution failed!', {
            timeout: toastTimeout,
          });
        } else if (queuedExecution.status === ExecutionStatus.SKIPPED) {
          ToastQueue.neutral('Code execution cannot run!', {
            timeout: toastTimeout,
          });
        } else {
          ToastQueue.positive('Code execution succeeded!', {
            timeout: toastTimeout,
          });
        }
      }
    } catch (error) {
      console.warn('Code execution state unknown:', error);
    }
  };

  const onAbort = async () => {
    if (!execution?.id) {
      console.warn('Code execution cannot be aborted as it is not running!');
      return;
    }
    try {
      await apiRequest<QueueOutput>({
        operation: 'Code execution aborting',
        url: `/apps/contentor/api/queue-code.json?jobId=${execution.id}`,
        method: 'delete',
      });
      clearInterval(pollExecutionRef.current!);
      setExecuting(false);

      let queuedExecution: Execution | null = null;
      while (queuedExecution === null || isExecutionPending(queuedExecution.status)) {
        const response = await apiRequest<QueueOutput>({
          operation: 'Code execution state',
          url: `/apps/contentor/api/queue-code.json?jobId=${execution.id}`,
          method: 'get',
        });
        queuedExecution = response.data.data.executions[0]!;
        setExecution(queuedExecution);
        await new Promise((resolve) => setTimeout(resolve, executionPollInterval));
      }
      if (queuedExecution.status === ExecutionStatus.ABORTED) {
        ToastQueue.positive('Code execution aborted successfully!', {
          timeout: toastTimeout,
        });
      } else {
        console.warn('Code execution aborting failed!');
        ToastQueue.negative('Code execution aborting failed!', {
          timeout: toastTimeout,
        });
      }
    } catch (error) {
      console.error('Code execution aborting error:', error);
      ToastQueue.negative('Code execution aborting failed!', {
        timeout: toastTimeout,
      });
    }
  };

  // Clear the interval on component unmount
  useEffect(() => {
    return () => {
      if (pollExecutionRef.current) {
        clearInterval(pollExecutionRef.current);
      }
    };
  }, []);

  useEffect(() => {
    setSyntaxError(undefined);
    setCompilationError(undefined);
    setIsCompiling(true);

    return () => {
      cancelCompilation();
    };
  }, [cancelCompilation, code]);

  const executionOutput = ((execution?.output ?? '') + '\n' + (execution?.error ?? '')).trim();

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
                      <Button variant="accent" onPress={onExecute} isPending={executing} isDisabled={isCompiling || !!syntaxError || !!compilationError}>
                        <Gears />
                        <Text>Execute</Text>
                      </Button>
                    </ButtonGroup>
                  </Flex>
                  <Flex flex="1" justifyContent="center" alignItems="center">
                    <CompilationStatus onCompilationErrorClick={() => setSelectedTab('output')} isCompiling={isCompiling} syntaxError={syntaxError} compilationError={compilationError} />
                  </Flex>
                  <Flex flex="1" justifyContent="end" alignItems="center">
                    <DialogTrigger>
                      <Button variant="secondary" style="fill">
                        <Help />
                        <Text>Help</Text>
                      </Button>
                      {(close) => (
                          <Dialog>
                            <Heading>Keyboard Shortcuts</Heading>
                            <Divider />
                            <Content>
                              <p>
                                <Keyboard>Fn</Keyboard> + <Keyboard>F1</Keyboard> &mdash; Command&nbsp;Palette
                              </p>
                              <p>
                                <Keyboard>⌃</Keyboard> + <Keyboard>Space</Keyboard> &mdash; Code&nbsp;Completions
                              </p>
                              <p>
                                <Keyboard>⌘</Keyboard> + <Keyboard>.</Keyboard> &mdash; Quick Fixes
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
                <ImmersiveEditor value={code} options={{ readOnly: executing }} onChange={setCode} syntaxError={syntaxError} language="groovy" beforeMount={registerGroovyLanguage} />
              </Flex>
            </Item>
            <Item key="output">
              <Flex direction="column" gap="size-200" marginY="size-100" flex={1}>
                <Flex direction="row" justifyContent="space-between" alignItems="center">
                  <Flex flex="1" alignItems="center">
                    <ButtonGroup>
                      <Button variant="negative" isDisabled={!executing} onPress={onAbort}>
                        <Cancel />
                        <Text>Abort</Text>
                      </Button>
                      <Button variant="secondary" isDisabled={!executionOutput} onPress={onCopyExecutionOutput}>
                        <Copy />
                        <Text>Copy</Text>
                      </Button>
                    </ButtonGroup>
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
                <ImmersiveEditor value={executionOutput} options={{ readOnly: true }} />
              </Flex>
            </Item>
          </TabPanels>
        </Tabs>
      </Flex>
  );
};

export default ConsolePage;