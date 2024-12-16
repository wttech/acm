import {
    Button,
    ButtonGroup,
    Flex,
    View,
    Text,
    Tabs,
    TabList,
    Item,
    TabPanels,
    Heading,
    Content,
    Keyboard,
    DialogTrigger,
    Dialog, Divider,
    ProgressBar
} from "@adobe/react-spectrum";
import Editor from "@monaco-editor/react";
import ConsoleCode from "./ConsoleCode.groovy";
import Spellcheck from "@spectrum-icons/workflow/Spellcheck";
import Gears from "@spectrum-icons/workflow/Gears";
import FileCode from "@spectrum-icons/workflow/FileCode";
import Print from "@spectrum-icons/workflow/Print";
import Copy from "@spectrum-icons/workflow/Copy";
import Cancel from "@spectrum-icons/workflow/Cancel";
import Bug from "@spectrum-icons/workflow/Bug";
import Help from "@spectrum-icons/workflow/Help";

import {ToastQueue} from '@react-spectrum/toast'
import {apiRequest} from "../utils/api.ts";
import React, {useState, useEffect, useRef} from "react";
import {registerGroovyLanguage} from "../utils/monaco/groovy.ts";
import {Execution} from "../utils/api.types.ts";
import {Strings} from "../utils/strings.ts";

const toastTimeout = 3000;
const executionPollDelay = 500;
const executionPollInterval = 500;
const executionFinalStatuses = ['SUCCEEDED', 'ABORTED', 'SKIPPED', 'FAILED', 'STOPPED'];

const ConsolePage = () => {
    const [selectedTab, setSelectedTab] = useState<string>('code');
    const [executing, setExecuting] = useState<boolean>(false);
    const [parsing, setParsing] = useState<boolean>(false);
    const [code, setCode] = useState<string | undefined>(ConsoleCode);
    const [execution, setExecution] = useState<Execution | null>(null);
    const pollExecutionRef = useRef<number | null>(null);

    const onExecute = async () => {
        setExecuting(true);
        setExecution(null);

        try {
            const response = await apiRequest<Execution>({
                operation: 'Code execution',
                url: `/apps/contentor/api/queue-code.json`,
                method: 'post',
                data: {
                    mode: 'evaluate',
                    code: {
                        id: 'console',
                        content: code,
                    }
                }
            });
            const queuedExecution = response.data.data;
            setExecution(queuedExecution);
            setSelectedTab('output');

            window.setTimeout(() => {
                pollExecutionRef.current = window.setInterval(() => {pollExecutionState(queuedExecution.id)}, executionPollInterval);
            }, executionPollDelay);
        } catch (error) {
            console.error('Code execution error:', error);
            setExecuting(false);
            ToastQueue.negative('Code execution error!', {timeout: toastTimeout});
        }
    };

    const pollExecutionState = async (jobId: string) => {
        try {
            const response = await apiRequest<Execution>({
                operation: 'Code execution state',
                url: `/apps/contentor/api/queue-code.json?jobId=${jobId}`,
                method: 'get'
            });
            const queuedExecution = response.data.data;
            setExecution(queuedExecution);

            if (executionFinalStatuses.includes(queuedExecution.status)) {
                clearInterval(pollExecutionRef.current!);
                setExecuting(false);
                if (queuedExecution.status === 'FAILED') {
                    ToastQueue.negative('Code execution failed!', {timeout: toastTimeout});
                    setSelectedTab('error');
                } else if (queuedExecution.status === 'SKIPPED') {
                    ToastQueue.neutral('Code execution cannot run!', {timeout: toastTimeout});
                } else {
                    ToastQueue.positive('Code execution succeeded!', {timeout: toastTimeout});
                    setSelectedTab('output');
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
            await apiRequest({
                operation: 'Code execution cancelling',
                url: `/apps/contentor/api/queue-code.json?jobId=${execution.id}`,
                method: 'delete'
            });
            clearInterval(pollExecutionRef.current!);
            setExecuting(false);

            let status = 'UNKNOWN';
            while (!executionFinalStatuses.includes(status)) {
                const response = await apiRequest<Execution>({
                    operation: 'Code execution state',
                    url: `/apps/contentor/api/queue-code.json?jobId=${execution.id}`,
                    method: 'get'
                });
                const queuedExecution = response.data.data;
                status = queuedExecution.status;

                setExecution(queuedExecution);
                if (status === 'STOPPED') {
                    setSelectedTab('output');
                    break;
                }
                await new Promise(resolve => setTimeout(resolve, executionPollInterval));
            }
            ToastQueue.info('Code execution aborted successfully!', {timeout: toastTimeout});
        } catch (error) {
            console.error('Code execution aborting error:', error);
            ToastQueue.negative('Code execution aborting failed!', {timeout: toastTimeout});
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

    const onParse = () => {
        setParsing(true);
        apiRequest<Execution>({
            operation: 'Code parsing',
            url: `/apps/contentor/api/execute-code.json`,
            method: 'post',
            data: {
                mode: 'parse',
                code: {
                    id: 'console',
                    content: code,
                }
            }
        }).then((response) => {
            const queuedExecution = response.data.data;
            setExecution(queuedExecution);

            if (queuedExecution.error) {
                ToastQueue.negative('Code parsing failed!', {timeout: toastTimeout});
                setSelectedTab('error');
            } else {
                ToastQueue.positive('Code parsing succeeded!', {timeout: toastTimeout});
            }
        }).catch(() => {
            setExecution(null);
            ToastQueue.negative('Code parsing error!', {timeout: toastTimeout});
        }).finally(() => {
            setParsing(false);
        });
    }

    const onCopyOutput = () => {
        if (execution?.output) {
            navigator.clipboard.writeText(execution.output)
                .then(() => {
                    ToastQueue.info('Output copied to clipboard!', {timeout: toastTimeout});
                })
                .catch(() => {
                    ToastQueue.negative('Failed to copy output!', {timeout: toastTimeout});
                });
        } else {
            ToastQueue.negative('No output to copy!', {timeout: toastTimeout});
        }
    };

    const onCopyError = () => {
        if (execution?.error) {
            navigator.clipboard.writeText(execution.error)
                .then(() => {
                    ToastQueue.info('Error copied to clipboard!', {timeout: toastTimeout});
                })
                .catch(() => {
                    ToastQueue.negative('Failed to copy error!', {timeout: toastTimeout});
                });
        } else {
            ToastQueue.negative('No error to copy!', {timeout: toastTimeout});
        }
    };

    return (
        <Flex direction="column" gap="size-200">
            <Tabs aria-label="Code execution" selectedKey={selectedTab} onSelectionChange={(key) => setSelectedTab(key as string)}>
                <TabList>
                    <Item key="code" aria-label="Code"><FileCode/><Text>Code</Text></Item>
                    <Item key="output" aria-label="Output"><Print/><Text>Output</Text></Item>
                    <Item key="error" aria-label="Error"><Bug/><Text>Error</Text></Item>
                </TabList>
                <TabPanels>
                    <Item key="code">
                        <Flex direction="column" gap="size-200" marginY="size-100">
                            <View>
                                <Flex justifyContent="space-between" alignItems="center">
                                    <ButtonGroup>
                                        <Button variant="accent" onPress={onExecute} isPending={executing}><Gears/><Text>Execute</Text></Button>
                                        <Button variant="secondary" onPress={onParse} isPending={parsing} style="fill"><Spellcheck/><Text>Parse</Text></Button>
                                    </ButtonGroup>
                                    <DialogTrigger>
                                        <Button variant="secondary" style="fill"><Help/><Text>Help</Text></Button>
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
                                                    <Button variant="secondary" onPress={close}>Close</Button>
                                                </ButtonGroup>
                                            </Dialog>
                                        )}
                                    </DialogTrigger>
                                </Flex>
                            </View>

                            <View backgroundColor="gray-800"
                                  borderWidth="thin"
                                  borderColor="dark"
                                  borderRadius="medium"
                                  padding="size-50">
                                <Editor theme="vs-dark"
                                        value={code}
                                        onChange={setCode}
                                        height="60vh"
                                        language="groovy"
                                        beforeMount={registerGroovyLanguage}
                                />
                            </View>
                        </Flex>
                    </Item>
                    <Item key="output">
                        <Flex direction="column" gap="size-200" marginY="size-100">
                            <Flex justifyContent="space-between" alignItems="center">
                                <Flex alignItems="center">
                                    <ButtonGroup>
                                        <Button variant="negative" isDisabled={!executing} onPress={onAbort}><Cancel/><Text>Abort</Text></Button>
                                        <Button variant="secondary" isDisabled={!execution?.output} onPress={onCopyOutput}><Copy/><Text>Copy</Text></Button>
                                    </ButtonGroup>
                                </Flex>
                                <Flex flex={1} justifyContent="center" alignItems="center">
                                    { execution ? (
                                        executing || !executionFinalStatuses.includes(execution.status) ? (
                                            <ProgressBar aria-label="Executing" showValueLabel={false} label="Executing…" isIndeterminate />
                                        ) : (
                                            <ProgressBar aria-label="Executed" showValueLabel={false} value={100} label={`${Strings.capitalize(execution.status)} after ${execution.duration} ms`}  isIndeterminate={executing} />
                                        )
                                    ) : (
                                        <ProgressBar aria-label="Not executing" label="Not executing" showValueLabel={false} value={0}/>
                                    )}
                                </Flex>
                                <Flex alignItems="center">
                                    <DialogTrigger>
                                        <Button variant="secondary" style="fill"><Help/><Text>Help</Text></Button>
                                        {(close) => (
                                            <Dialog>
                                                <Heading>Code execution</Heading>
                                                <Divider />
                                                <Content>
                                                    <p><Print size="XS" /> Output is printed live.</p>
                                                    <p><Cancel size="XS" /> <Text>Abort if the execution:</Text>
                                                        <ul style={{listStyleType: 'none'}}>
                                                            <li><Spellcheck size="XS" /> is taking too long</li>
                                                            <li><Bug size="XS" /> is stuck in an infinite loop</li>
                                                            <li><Gears size="XS" /> makes the instance unresponsive</li>
                                                        </ul>
                                                    </p>
                                                    <p><Help size="XS" /> Be aware that aborting execution may leave data in an inconsistent state.</p>
                                                </Content>
                                                <ButtonGroup>
                                                    <Button variant="secondary" onPress={close}>Close</Button>
                                                </ButtonGroup>
                                            </Dialog>
                                        )}
                                    </DialogTrigger>
                                </Flex>
                            </Flex>
                            <View backgroundColor="gray-800"
                                  borderWidth="thin"
                                  borderColor="dark"
                                  borderRadius="medium"
                                  padding="size-50">
                                <Editor theme="vs-dark"
                                        value={execution?.output ?? ''}
                                        height="60vh"
                                        options={{readOnly: true}}
                                />
                            </View>
                        </Flex>
                    </Item>
                    <Item key="error">
                        <Flex direction="column" gap="size-200" marginY="size-100">
                            <ButtonGroup>
                                <Button variant="secondary" isDisabled={!execution?.error} onPress={onCopyError}><Copy/><Text>Copy</Text></Button>
                            </ButtonGroup>
                            <View backgroundColor="gray-800"
                                  borderWidth="thin"
                                  borderColor="dark"
                                  borderRadius="medium"
                                  padding="size-50">
                                <Editor theme="vs-dark"
                                        value={execution?.error ?? ''}
                                        height="60vh"
                                        options={{readOnly: true}}
                                />
                            </View>
                        </Flex>
                    </Item>
                </TabPanels>
            </Tabs>
        </Flex>
    );
};

export default ConsolePage;
