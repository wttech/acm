import {useEffect, useState} from 'react';
import {useInterval} from 'react-use';
import {
  ButtonGroup,
  Flex,
  Item,
  Switch,
  TabList,
  TabPanels,
  Tabs,
  Text
} from '@adobe/react-spectrum';
import {ToastQueue} from '@react-spectrum/toast';
import FileCode from '@spectrum-icons/workflow/FileCode';
import Print from '@spectrum-icons/workflow/Print';
import CompilationStatus from '../components/CompilationStatus.tsx';
import ExecutionAbortButton from '../components/ExecutionAbortButton';
import ExecutionCopyOutputButton from '../components/ExecutionCopyOutputButton';
import ExecutionProgressBar from '../components/ExecutionProgressBar';
import ImmersiveEditor from '../components/ImmersiveEditor';
import KeyboardShortcutsButton from '../components/KeyboardShortcutsButton';
import {apiRequest} from '../utils/api.ts';
import {
  ArgumentValues,
  Description,
  Execution,
  ExecutionStatus,
  isExecutionPending,
  QueueOutput
} from '../utils/api.types.ts';
import {StorageKeys} from '../utils/storage.ts';
import ConsoleCode from './ConsoleCode.groovy';
import CodeExecuteButton from "../components/CodeExecuteButton";
import {useCompilation} from '../hooks/code';
import ConsoleHelpButton from "../components/ConsoleHelpButton.tsx";

const toastTimeout = 3000;
const executionPollInterval = 500;
type SelectedTab = 'code' | 'output';

const ConsolePage = () => {
  const [selectedTab, setSelectedTab] = useState<SelectedTab>('code');
  const [executing, setExecuting] = useState<boolean>(false);
  const [code, setCode] = useState<string | undefined>(localStorage.getItem(StorageKeys.EDITOR_CODE) || ConsoleCode);
  const [autoscroll, setAutoscroll] = useState<boolean>(true);
  const [execution, setExecution] = useState<Execution | null>(null);
  const [compiling, syntaxError, compileError, parseExecution ] = useCompilation(code);

  useEffect(() => {
    setExecution(parseExecution)
  }, [parseExecution]);

  const onDescribeFailed = (description: Description) => {
    console.error("Code description failed:", description);
    setExecution(description.execution)
    setSelectedTab('output')
    ToastQueue.negative('Code description failed!', {
      timeout: toastTimeout,
    });
  }

  const onExecute = async (description: Description, args: ArgumentValues) => {
    setExecuting(true);
    setExecution(null);

    try {
      const response = await apiRequest<QueueOutput>({
        operation: 'Code execution',
        url: `/apps/acm/api/queue-code.json`,
        method: 'post',
        data: {
          code: {
            id: 'console',
            content: code,
            arguments: args
          },
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
        if (executing && execution && isExecutionPending(execution.status)) {
          pollExecutionState(execution.id);
        }
      },
      executing && execution && isExecutionPending(execution.status) ? executionPollInterval : null,
  );

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
                      <CodeExecuteButton code={code || ''} onDescribeFailed={onDescribeFailed} onExecute={onExecute} isPending={executing || compiling} isDisabled={!!syntaxError || !!compileError}/>
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
                    <ConsoleHelpButton/>
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