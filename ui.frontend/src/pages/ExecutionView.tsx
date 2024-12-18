import React, {useEffect, useState} from 'react';
import {
    Button,
    ButtonGroup,
    Content,
    Flex,
    IllustratedMessage,
    Item,
    LabeledValue,
    TabList,
    TabPanels,
    Tabs,
    Text,
    View
} from "@adobe/react-spectrum";
import {Field} from '@react-spectrum/label';
import {Execution, ExecutionOutput} from '../utils/api.types';
import {toastRequest} from '../utils/api';
import NotFound from "@spectrum-icons/illustrations/NotFound";
import History from "@spectrum-icons/workflow/History";
import ExecutionStatusBadge from "../components/ExecutionStatusBadge.tsx";
import {Strings} from "../utils/strings.ts";
import FileCode from "@spectrum-icons/workflow/FileCode";
import Print from "@spectrum-icons/workflow/Print";
import Editor from "@monaco-editor/react";
import {registerGroovyLanguage} from "../utils/monaco/groovy.ts";
import ExecutableValue from "../components/ExecutableValue.tsx";
import Copy from "@spectrum-icons/workflow/Copy";
import {ToastQueue} from "@react-spectrum/toast";
import {useParams} from "react-router-dom";

const toastTimeout = 3000;

const ExecutionView = () => {
    const [execution, setExecution] = useState<Execution | null>(null);
    const executionId = decodeURIComponent(useParams<{ executionId: string }>().executionId as string);

    useEffect(() => {
        const fetchExecution = async () => {
            try {
                const response = await toastRequest<ExecutionOutput>({
                    method: 'GET',
                    url: `/apps/contentor/api/execution.json?id=${executionId}`,
                    operation: `Executions loading`,
                    positive: false
                });
                setExecution(response.data.data.list[0]);
            } catch (error) {
                console.error('Error fetching executions:', error);
            }
        };
        fetchExecution();
    }, [executionId]);

    if (!execution) {
        return (
            <Flex justifyContent="center" alignItems="center" height="100vh">
                <IllustratedMessage>
                    <NotFound />
                    <Content>Execution not found</Content>
                </IllustratedMessage>
            </Flex>
        )
    }

    const executionOutput = ((execution.output ?? '') + '\n' + (execution.error ?? '')).trim();

    const onCopyExecutionOutput = () => {
        if (executionOutput) {
            navigator.clipboard.writeText(executionOutput)
                .then(() => {
                    ToastQueue.info('Execution output copied to clipboard!', {timeout: toastTimeout});
                })
                .catch(() => {
                    ToastQueue.negative('Failed to copy execution output!', {timeout: toastTimeout});
                });
        } else {
            ToastQueue.negative('No execution output to copy!', {timeout: toastTimeout});
        }
    };

    const onCopyExecutableCode = () => {
        navigator.clipboard.writeText(execution.executable.content)
            .then(() => {
                ToastQueue.info('Execution code copied to clipboard!', {timeout: toastTimeout});
            })
            .catch(() => {
                ToastQueue.negative('Failed to copy execution code!', {timeout: toastTimeout});
            });
    };

    return (
        <Flex direction="column" gap="size-400">
            <Tabs aria-label='Executions'>
                <TabList>
                    <Item key="details"><History/><Text>Execution</Text></Item>
                    <Item key="code" aria-label="Code"><FileCode/><Text>Code</Text></Item>
                    <Item key="output" aria-label="Execution"><Print/><Text>Output</Text></Item>
                </TabList>
                <TabPanels>
                    <Item key="details">
                        <Flex direction="column" gap="size-200" marginY="size-100">
                            <View backgroundColor="gray-50" padding="size-200" borderRadius="medium" borderColor="dark" borderWidth="thin">
                                <Flex direction="row" justifyContent="space-between" gap="size-200">
                                    <LabeledValue label="ID" value={execution.id} />
                                    <Field label="Status">
                                        <div><ExecutionStatusBadge value={execution.status}/></div>
                                    </Field>
                                </Flex>
                            </View>
                            <View backgroundColor="gray-50" padding="size-200" borderRadius="medium" borderColor="dark" borderWidth="thin">
                                <Field label="Executable" width="100%">
                                    <div>
                                        <ExecutableValue value={execution.executable}/>
                                    </div>
                                </Field>
                            </View>
                            <View backgroundColor="gray-50" padding="size-200" borderRadius="medium" borderColor="dark" borderWidth="thin">
                                <Flex direction="row" justifyContent="space-between" gap="size-200">
                                    <LabeledValue label="Started At" value={Strings.dateExplained(execution.startDate)} />
                                    <LabeledValue label="Duration" value={Strings.durationExplained(execution.duration)} />
                                    <LabeledValue label="Ended At" value={Strings.dateExplained(execution.endDate)} />
                                </Flex>
                            </View>
                        </Flex>
                    </Item>
                    <Item key="code">
                        <Flex direction="column" gap="size-200" marginY="size-100">
                            <View>
                                <Flex justifyContent="space-between" alignItems="center">
                                    <ButtonGroup>
                                        <Button variant="secondary" isDisabled={!executionOutput} onPress={onCopyExecutableCode}><Copy/><Text>Copy</Text></Button>
                                    </ButtonGroup>
                                </Flex>
                            </View>
                            <View backgroundColor="gray-800"
                                  borderWidth="thin"
                                  borderColor="dark"
                                  borderRadius="medium"
                                  padding="size-50">
                                <Editor theme="vs-dark"
                                    value={execution.executable.content}
                                    height="60vh"
                                    language="groovy"
                                    beforeMount={registerGroovyLanguage}
                                    options={{readOnly: true}}
                                />
                            </View>
                        </Flex>
                    </Item>
                    <Item key="output">
                        <Flex direction="column" gap="size-200" marginY="size-100">
                            <View>
                                <Flex justifyContent="space-between" alignItems="center">
                                    <ButtonGroup>
                                        <Button variant="secondary" isDisabled={!executionOutput} onPress={onCopyExecutionOutput}><Copy/><Text>Copy</Text></Button>
                                    </ButtonGroup>
                                </Flex>
                            </View>
                            <View backgroundColor="gray-800"
                                  borderWidth="thin"
                                  borderColor="dark"
                                  borderRadius="medium"
                                  padding="size-50">
                                <Editor theme="vs-dark"
                                        value={executionOutput}
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

export default ExecutionView;
