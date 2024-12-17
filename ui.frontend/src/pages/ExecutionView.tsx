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

const ExecutionView = () => {
    const [execution, setExecution] = useState<Execution | null>(null);

    const executionId = '2024-12-17-08-40-e39746ba-091a-458a-91e2-9c6184186375_112000000';

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
    }, []);

    if (execution === null) {
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
                            <View backgroundColor="gray-100" padding="size-200" borderRadius="medium" borderColor="dark" borderWidth="thin">
                                <LabeledValue label="ID" value={execution.id} />
                            </View>
                            <View backgroundColor="gray-100" padding="size-200" borderRadius="medium" borderColor="dark" borderWidth="thin">
                                <Field label="Executable" width="100%">
                                    <div>
                                        <ExecutableValue value={execution.executable}/>
                                    </div>
                                </Field>
                            </View>
                            <View backgroundColor="gray-100" padding="size-200" borderRadius="medium" borderColor="dark" borderWidth="thin">
                                <Field label="Status">
                                    <div><ExecutionStatusBadge value={execution.status}/></div>
                                </Field>
                            </View>
                            <View backgroundColor="gray-100" padding="size-200" borderRadius="medium" borderColor="dark" borderWidth="thin">
                                <LabeledValue label="Started At" value={`${Strings.date(execution.startDate)} (${Strings.dateRelative(execution.startDate)})`} />
                            </View>
                            <View backgroundColor="gray-100" padding="size-200" borderRadius="medium" borderColor="dark" borderWidth="thin">
                                <LabeledValue label="Ended At" value={`${Strings.date(execution.endDate)} (${Strings.dateRelative(execution.endDate)})`} />
                            </View>
                            <View backgroundColor="gray-100" padding="size-200" borderRadius="medium" borderColor="dark" borderWidth="thin">
                                <LabeledValue label="Duration" value={Strings.duration(execution.duration)} />
                            </View>
                        </Flex>
                    </Item>
                    <Item key="code">
                        <Flex direction="column" gap="size-200" marginY="size-100">
                            <View>
                                <Flex justifyContent="space-between" alignItems="center">
                                    <ButtonGroup>
                                        <Button variant="secondary">Copy</Button>
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
                                        <Button variant="secondary">Copy</Button>
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
