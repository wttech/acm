import {Button, ButtonGroup, Flex, View, Text, Tabs, TabList, Item, TabPanels} from "@adobe/react-spectrum";
import Editor from "@monaco-editor/react";
import ConsoleCode from "./ConsoleCode.groovy";
import Spellcheck from "@spectrum-icons/workflow/Spellcheck";
import Gears from "@spectrum-icons/workflow/Gears";
import FileCode from "@spectrum-icons/workflow/FileCode";
import Print from "@spectrum-icons/workflow/Print";
import Copy from "@spectrum-icons/workflow/Copy";
import Cancel from "@spectrum-icons/workflow/Cancel";
import Bug from "@spectrum-icons/workflow/Bug";

import {ToastQueue} from '@react-spectrum/toast'
import {apiRequest} from "../utils/api.ts";
import {useState, useEffect, useRef} from "react";
import {registerGroovyLanguage} from "../utils/monaco.groovy.ts";

const ConsolePage = () => {
    const [selectedTab, setSelectedTab] = useState<string>('code');
    const [executing, setExecuting] = useState<boolean>(false);
    const [parsing, setParsing] = useState<boolean>(false);
    const [code, setCode] = useState<string | undefined>(ConsoleCode);
    const [output, setOutput] = useState<string | undefined>('');
    const [error, setError] = useState<string | undefined>('');
    const [jobId, setJobId] = useState<string | undefined>(undefined);
    const pollingRef = useRef<number | null>(null);

    const onExecute = async () => {
        setExecuting(true);
        try {
            const response = await apiRequest({
                operation: 'Code execution',
                url: `/apps/migrator/api/queue-code.json`,
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
            pollingRef.current = window.setInterval(() => onPoll(jobId), 1000);
        } catch (error) {
            setExecuting(false);
            ToastQueue.negative('Code execution error!', {timeout: 3000});
        }
    };

    const onPoll = async (jobId: string) => {
        try {
            const response = await apiRequest({
                operation: 'Code execution state',
                url: `/apps/migrator/api/queue-code.json?jobId=${jobId}`,
                method: 'get'
            });
            const responseData = response.data;
            const executionJob = responseData.data;

            setOutput(executionJob.output);
            if (executionJob.state === 'ACTIVE') {
                setSelectedTab('output');
            }
            if (['SUCCEEDED', 'FAILED', 'STOPPED'].includes(executionJob.state)) {
                clearInterval(pollingRef.current!);
                setExecuting(false);
                if (executionJob.state === 'FAILED') {
                    ToastQueue.negative('Code execution failed!', {timeout: 3000});
                    setSelectedTab('error');
                } else {
                    ToastQueue.positive('Code execution succeeded!', {timeout: 3000});
                }
            }
        } catch (error) {
            clearInterval(pollingRef.current!);
            setExecuting(false);
            ToastQueue.negative('Code execution state error!', {timeout: 3000});
        }
    };

    const onAbort = async () => {
        if (jobId) {
            try {
                await apiRequest({
                    operation: 'Code execution cancelling',
                    url: `/apps/migrator/api/queue-code.json?jobId=${jobId}`,
                    method: 'delete'
                });
                clearInterval(pollingRef.current!);
                setExecuting(false);
                ToastQueue.neutral('Code execution cancelled successfully!', {timeout: 3000});
            } catch (error) {
                ToastQueue.negative('Code execution cancelling failed!', {timeout: 3000});
            }
        }
    };

    useEffect(() => {
        return () => {
            if (pollingRef.current) {
                clearInterval(pollingRef.current);
            }
        };
    }, []);

    const onParse = () => {
        setParsing(true);
        apiRequest({
            operation: 'Script parsing',
            url: `/apps/migrator/api/execute-code.json`,
            method: 'post',
            data: {
                mode: 'parse',
                code: {
                    id: 'console',
                    content: code,
                }
            }
        }).then((response: any) => {
            const responseData = response.data;
            const execution = responseData.data;

            setOutput(execution.output);
            setError(execution.error);

            if (execution.error) {
                ToastQueue.negative('Code parsing failed!', {timeout: 3000});
                setSelectedTab('error');
            } else {
                ToastQueue.positive('Code parsing succeeded!', {timeout: 3000});
            }
        }).catch(() => {
            setOutput('');
            setError('');
            ToastQueue.negative('Code parsing error!', {timeout: 3000});
        }).finally(() => {
            setParsing(false);
        });
    }

    const onCopyOutput = () => {
        ToastQueue.neutral('Copy output to be implemented!', {timeout: 5000});
    }
    const onCopyError = () => {
        ToastQueue.neutral('Copy error to be implemented!', {timeout: 5000});
    }

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
                            <ButtonGroup>
                                <Button variant="accent" onPress={onExecute} isPending={executing}><Gears/><Text>Execute</Text></Button>
                                <Button variant="secondary" onPress={onParse} isPending={parsing} style="fill"><Spellcheck/><Text>Parse</Text></Button>
                            </ButtonGroup>
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
                                <Button variant="secondary" onPress={onCopyOutput}><Copy/><Text>Copy</Text></Button>
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
                                <Button variant="secondary" onPress={onCopyError}><Copy/><Text>Copy</Text></Button>
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
