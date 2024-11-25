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
    Dialog, Divider
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
import {ApiDataExecution, apiRequest} from "../utils/api.ts";
import React, {useState, useEffect, useRef} from "react";
import {registerGroovyLanguage} from "../utils/monaco/groovy.ts";

const pollInterval = 500;
const toastTimeout = 3000;

const ConsolePage = () => {
    const [selectedTab, setSelectedTab] = useState<string>('code');
    const [executing, setExecuting] = useState<boolean>(false);
    const [parsing, setParsing] = useState<boolean>(false);
    const [code, setCode] = useState<string | undefined>(ConsoleCode);
    const [output, setOutput] = useState<string | undefined>('');
    const [error, setError] = useState<string | undefined>('');
    const [jobId, setJobId] = useState<string | undefined>(undefined);
    const pollExecutionRef = useRef<number | null>(null);

    const onExecute = async () => {
        setExecuting(true);
        try {
            const response = await apiRequest<ApiDataExecution>({
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
            const executionJob = response.data;
            const jobId = executionJob.data.id;
            setJobId(jobId);
            pollExecutionRef.current = window.setInterval(() => pollExecutionState(jobId), pollInterval);
        } catch (error) {
            console.error('Code execution error:', error);
            setExecuting(false);
            ToastQueue.negative('Code execution error!', {timeout: toastTimeout});
        }
    };

    const pollExecutionState = async (jobId: string) => {
        try {
            const response = await apiRequest<ApiDataExecution>({
                operation: 'Code execution state',
                url: `/apps/contentor/api/queue-code.json?jobId=${jobId}`,
                method: 'get'
            });
            const responseData = response.data;
            const executionJob = responseData.data;

            setOutput(executionJob.output);
            setError(executionJob.error);

            if (executionJob.status === 'ACTIVE') {
                setSelectedTab('output');
            }
            if (['SUCCEEDED', 'FAILED', 'STOPPED'].includes(executionJob.status)) {
                clearInterval(pollExecutionRef.current!);
                setExecuting(false);
                if (executionJob.status === 'FAILED') {
                    ToastQueue.negative('Code execution failed!', {timeout: toastTimeout});
                    setSelectedTab('error');
                } else {
                    ToastQueue.positive('Code execution succeeded!', {timeout: toastTimeout});
                }
            }
        } catch (error) {
            console.error('Code execution state error:', error);
            clearInterval(pollExecutionRef.current!);
            setExecuting(false);
            ToastQueue.negative('Code execution state error!', {timeout: toastTimeout});
        }
    };

    const onAbort = async () => {
        if (!jobId) {
            console.warn('Code execution cannot be aborted as it is not running!');
            return
        }
        try {
            await apiRequest({
                operation: 'Code execution cancelling',
                url: `/apps/contentor/api/queue-code.json?jobId=${jobId}`,
                method: 'delete'
            });
            clearInterval(pollExecutionRef.current!);
            setExecuting(false);

            let status = 'UNKNOWN';
            while (!['STOPPED', 'FAILED', 'SUCCEEDED'].includes(status)) {
                const response = await apiRequest<ApiDataExecution>({
                    operation: 'Code execution state',
                    url: `/apps/contentor/api/queue-code.json?jobId=${jobId}`,
                    method: 'get'
                });
                const responseData = response.data;
                const execution = responseData.data;
                status = execution.status;

                setOutput(execution.output);
                setError(execution.error);
                if (status === 'STOPPED') {
                    setSelectedTab('output');
                    break;
                }
                await new Promise(resolve => setTimeout(resolve, pollInterval));
            }
            ToastQueue.neutral('Code execution cancelled successfully!', {timeout: toastTimeout});
        } catch (error) {
            console.error('Code execution cancelling error:', error);
            ToastQueue.negative('Code execution cancelling failed!', {timeout: toastTimeout});
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
        apiRequest<ApiDataExecution>({
            operation: 'Script parsing',
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
            const responseData = response.data;
            const execution = responseData.data;

            setOutput(execution.output);
            setError(execution.error);

            if (execution.error) {
                ToastQueue.negative('Code parsing failed!', {timeout: toastTimeout});
                setSelectedTab('error');
            } else {
                ToastQueue.positive('Code parsing succeeded!', {timeout: toastTimeout});
            }
        }).catch(() => {
            setOutput('');
            setError('');
            ToastQueue.negative('Code parsing error!', {timeout: toastTimeout});
        }).finally(() => {
            setParsing(false);
        });
    }

    const onCopyOutput = () => {
        if (output) {
            navigator.clipboard.writeText(output)
                .then(() => {
                    ToastQueue.neutral('Output copied to clipboard!', {timeout: toastTimeout});
                })
                .catch(() => {
                    ToastQueue.negative('Failed to copy output!', {timeout: toastTimeout});
                });
        } else {
            ToastQueue.negative('No output to copy!', {timeout: toastTimeout});
        }
    };

    const onCopyError = () => {
        if (error) {
            navigator.clipboard.writeText(error)
                .then(() => {
                    ToastQueue.neutral('Error copied to clipboard!', {timeout: toastTimeout});
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
            <Tabs selectedKey={selectedTab} onSelectionChange={(key) => setSelectedTab(key as string)}>
                <TabList>
                    <Item key="code"><FileCode/><Text>Code</Text></Item>
                    <Item key="output"><Print/><Text>Output</Text></Item>
                    <Item key="error"><Bug/><Text>Error</Text></Item>
                </TabList>
                <TabPanels>
                    <Item key="code">
                        <Flex direction="column" gap="size-200" marginY="size-100">
                            <View>
                                <ButtonGroup>
                                    <Button variant="accent" onPress={onExecute} isPending={executing}><Gears/><Text>Execute</Text></Button>
                                    <Button variant="secondary" onPress={onParse} isPending={parsing} style="fill"><Spellcheck/><Text>Parse</Text></Button>
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
                                </ButtonGroup>
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
                            <ButtonGroup>
                                <Button variant="negative" isDisabled={!executing} onPress={onAbort}><Cancel/><Text>Abort</Text></Button>
                                <Button variant="secondary" isDisabled={!output} onPress={onCopyOutput}><Copy/><Text>Copy</Text></Button>
                            </ButtonGroup>
                            <View backgroundColor="gray-800"
                                  borderWidth="thin"
                                  borderColor="dark"
                                  borderRadius="medium"
                                  padding="size-50">
                                <Editor theme="vs-dark"
                                        value={output}
                                        height="60vh"
                                        options={{readOnly: true}}
                                />
                            </View>
                        </Flex>
                    </Item>
                    <Item key="error">
                        <Flex direction="column" gap="size-200" marginY="size-100">
                            <ButtonGroup>
                                <Button variant="secondary" isDisabled={!error} onPress={onCopyError}><Copy/><Text>Copy</Text></Button>
                            </ButtonGroup>
                            <View backgroundColor="gray-800"
                                  borderWidth="thin"
                                  borderColor="dark"
                                  borderRadius="medium"
                                  padding="size-50">
                                <Editor theme="vs-dark"
                                        value={error}
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
