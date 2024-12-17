import React, {useEffect, useState} from 'react';
import {Content, Flex, IllustratedMessage, Item, TabList, TabPanels, Tabs, Text} from "@adobe/react-spectrum";
import {Execution, ExecutionOutput} from '../utils/api.types';
import {toastRequest} from '../utils/api';
import NotFound from "@spectrum-icons/illustrations/NotFound";
import Folder from "@spectrum-icons/workflow/Folder";
import ExecutionStatusBadge from "../components/ExecutionStatusBadge.tsx";
import {Strings} from "../utils/strings.ts";
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

    return (
        <Flex direction="column" gap="size-400">
            <Tabs aria-label='Executions'>
                <TabList>
                    <Item key="executable">
                        <Folder/>
                        <Text>Executable</Text>
                    </Item>
                    <Item key="details">
                        <Folder/>
                        <Text>Details</Text>
                    </Item>
                </TabList>
                <TabPanels>
                    <Item key="executable">
                        <ExecutableValue value={execution.executable} />
                    </Item>
                    <Item key="details">
                        <p><ExecutionStatusBadge value={execution.status} /></p>
                        <p>Started At: {Strings.date(execution.startDate)}</p>
                        <p>Ended At: {Strings.date(execution.endDate)}</p>
                    </Item>
                </TabPanels>
            </Tabs>
        </Flex>
    );
};

export default ExecutionView;
