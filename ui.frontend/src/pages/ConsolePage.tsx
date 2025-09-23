import { ButtonGroup, Flex, Item, Switch, TabList, TabPanels, Tabs, Text } from '@adobe/react-spectrum';
import { ToastQueue } from '@react-spectrum/toast';
import FileCode from '@spectrum-icons/workflow/FileCode';
import Print from '@spectrum-icons/workflow/Print';
import { useEffect, useState } from 'react';
import CodeEditor from '../components/CodeEditor';
import CodeExecuteButton from '../components/CodeExecuteButton';
import CodeSaveButton from '../components/CodeSaveButton';
import CompilationStatus from '../components/CompilationStatus';
import ConsoleHelpButton from '../components/ConsoleHelpButton';
import ExecutionAbortButton from '../components/ExecutionAbortButton';
import ExecutionCopyOutputButton from '../components/ExecutionCopyOutputButton';
import ExecutionProgressBar from '../components/ExecutionProgressBar';
import KeyboardShortcutsButton from '../components/KeyboardShortcutsButton';
import ScriptExecutorStatusLight from '../components/ScriptExecutorStatusLight';
import Toggle from '../components/Toggle';
import { useAppState } from '../hooks/app';
import { useCompilation } from '../hooks/code';
import { useExecutionPolling } from '../hooks/execution';
import { apiRequest, toastRequest } from '../utils/api';
import { ConsoleDefaultScriptContent, ConsoleDefaultScriptPath, Description, ExecutableIdConsole, Execution, InputValues, isExecutionPending, QueueOutput, ScriptOutput } from '../utils/api.types.ts';
import { GROOVY_LANGUAGE_ID } from '../utils/monaco/groovy.ts';
import { LOG_LANGUAGE_ID } from '../utils/monaco/log.ts';
import { ToastTimeoutQuick } from '../utils/spectrum.ts';
import { StorageKeys } from '../utils/storage';

const ConsolePage = () => {
  const appState = useAppState();
  const pausedExecution = !appState.healthStatus.healthy;

  const [selectedTab, setSelectedTab] = useState<'code' | 'output'>('code');
  const [code, setCode] = useState<string | undefined>(() => localStorage.getItem(StorageKeys.EDITOR_CODE) || undefined);
  const [compiling, pendingCompile, syntaxError, compileError, parseExecution] = useCompilation(code, (newCode) => localStorage.setItem(StorageKeys.EDITOR_CODE, newCode));
  const [queuedExecution, setQueuedExecution] = useState<Execution | null>(null);

  const { execution, setExecution, executing, setExecuting } = useExecutionPolling(queuedExecution?.id, appState.spaSettings.executionPollInterval);
  const [autoscroll, setAutoscroll] = useState<boolean>(true);

  useEffect(() => {
    if (code === undefined) {
      toastRequest<ScriptOutput>({
        method: 'GET',
        url: `/apps/acm/api/script.json?id=${ConsoleDefaultScriptPath}`,
        operation: 'Loading console default script',
        positive: false,
      })
        .then((response) => {
          const scriptOutput = response.data.data;
          const scriptDefault = scriptOutput.list?.[0];
          if (scriptDefault) {
            setCode(scriptDefault.content);
          } else {
            const errorMessage = `Loading console default script failed! Not found at path '${ConsoleDefaultScriptPath}'`;
            console.warn(errorMessage);
            ToastQueue.negative(errorMessage);
            setCode(ConsoleDefaultScriptContent);
          }
        })
        .catch((error) => {
          console.error('Loading console default script failed!', error);
        });
    }
  }, [code]);

  useEffect(() => {
    setExecution(parseExecution);
  }, [parseExecution, setExecution]);

  useEffect(() => {
    if (!isExecutionPending(queuedExecution?.status)) {
      setExecuting(false);
    }
  }, [queuedExecution, setExecuting]);

  const onDescribeFailed = (description: Description) => {
    console.error('Code description failed:', description);
    setExecution(description.execution);
    setSelectedTab('output');
    ToastQueue.negative('Code description failed!', { timeout: ToastTimeoutQuick });
  };

  const onExecute = async (description: Description, inputs: InputValues) => {
    setExecuting(true);
    setExecution(null);

    try {
      const response = await apiRequest<QueueOutput>({
        operation: 'Code execution',
        url: `/apps/acm/api/queue-code.json`,
        method: 'post',
        data: {
          code: {
            id: ExecutableIdConsole,
            content: code,
            inputs: inputs,
          },
        },
      });
      const queuedExecution = response.data.data.executions[0]!;
      setQueuedExecution(queuedExecution);
      setExecution(queuedExecution);
      setSelectedTab('output');
    } catch (error) {
      console.error('Code execution error:', error);
      setExecuting(false);
      ToastQueue.negative('Code execution error!', { timeout: ToastTimeoutQuick });
    }
  };

  const executionOutput = ((execution?.output ?? '') + '\n' + (execution?.error ?? '')).trim();
  const executableNotReady = pausedExecution || pendingCompile || !!syntaxError || !!compileError;

  return (
    <Flex direction="column" flex="1" gap="size-200">
      <Tabs flex="1" aria-label="Code execution" selectedKey={selectedTab} onSelectionChange={(key) => setSelectedTab(key as 'code' | 'output')}>
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
          <Item key="code" aria-label="Code">
            <Flex direction="column" gap="size-200" marginY="size-100" flex={1}>
              <Flex direction="row" justifyContent="space-between" alignItems="center">
                <Flex flex="1" alignItems="center">
                  <ButtonGroup>
                    <CodeExecuteButton code={code || ''} onDescribeFailed={onDescribeFailed} onExecute={onExecute} isPending={executing || compiling} isDisabled={executableNotReady} />
                    <Toggle when={appState.spaSettings.scriptManagementEnabled}>
                      <CodeSaveButton code={code || ''} variant="secondary" isDisabled={executableNotReady} />
                    </Toggle>
                  </ButtonGroup>
                </Flex>
                <Flex flex="1" justifyContent="center" alignItems="center">
                  {pausedExecution ? <ScriptExecutorStatusLight /> : <CompilationStatus onErrorClick={() => setSelectedTab('output')} compiling={compiling} syntaxError={syntaxError} compileError={compileError} />}
                </Flex>
                <Flex flex="1" justifyContent="end" alignItems="center">
                  <KeyboardShortcutsButton />
                </Flex>
              </Flex>
              <CodeEditor id="code-editor" initialValue={code} readOnly={executing} onChange={setCode} syntaxError={syntaxError} language={GROOVY_LANGUAGE_ID} />
            </Flex>
          </Item>
          <Item key="output" aria-label="Output">
            <Flex direction="column" gap="size-200" marginY="size-100" flex={1}>
              <Flex direction="row" justifyContent="space-between" alignItems="center">
                <Flex flex="1" alignItems="center">
                  <ButtonGroup>
                    <ExecutionAbortButton execution={execution} onComplete={setExecution} />
                    <ExecutionCopyOutputButton output={executionOutput} />
                  </ButtonGroup>
                </Flex>
                <Flex flex="1" justifyContent="center" alignItems="center">
                  <ExecutionProgressBar execution={execution} active={executing} />
                </Flex>
                <Flex flex="1" justifyContent="end" alignItems="center">
                  <Switch isSelected={autoscroll} isDisabled={!isExecutionPending(execution?.status)} marginStart={20} onChange={() => setAutoscroll((prev) => !prev)}>
                    <Text>Autoscroll</Text>
                  </Switch>
                  <ConsoleHelpButton />
                </Flex>
              </Flex>
              <CodeEditor id="output-preview" value={executionOutput} readOnly scrollToBottomOnUpdate={autoscroll} language={LOG_LANGUAGE_ID} />
            </Flex>
          </Item>
        </TabPanels>
      </Tabs>
    </Flex>
  );
};

export default ConsolePage;
