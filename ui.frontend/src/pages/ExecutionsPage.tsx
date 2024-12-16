import React, { useEffect, useState } from 'react';
import {
    Cell,
    Column,
    Flex,
    Row,
    TableBody,
    TableHeader,
    TableView,
    Tabs,
    TabList,
    TabPanels,
    Item,
    Text,
    IllustratedMessage, Content, ProgressBar
} from "@adobe/react-spectrum";
import { ExecutionOutput } from '../utils/api.types';
import { toastRequest } from '../utils/api';
import NotFound from "@spectrum-icons/illustrations/NotFound";
import ExecutionStatus from "../components/ExecutionStatus.tsx";

const ExecutionsPage = () => {
    const [executions, setExecutions] = useState<ExecutionOutput | null>(null);

    useEffect(() => {
        const fetchExecutions = async () => {
            try {
                const response = await toastRequest<ExecutionOutput>({
                    method: 'GET',
                    url: `/apps/contentor/api/execution.json`,
                    operation: `Executions loading`,
                    positive: false
                });
                setExecutions(response.data.data);
            } catch (error) {
                console.error('Error fetching executions:', error);
            }
        };
        fetchExecutions();
    }, []);

    const renderEmptyState = () => (
        <IllustratedMessage>
            <NotFound />
            <Content>No scripts found</Content>
        </IllustratedMessage>
    );

    if (executions === null) {
        return (
            <Flex justifyContent="center" alignItems="center" height="100vh">
                <ProgressBar label="Loading..." isIndeterminate />
            </Flex>
        )
    }

    return (
        <Flex direction="column" gap="size-400">
            <Tabs aria-label='Executions'>
                <TabList>
                    <Item aria-label="Today" key="today"><Text>Today</Text></Item>
                </TabList>
                <TabPanels>
                    <Item key="today">
                        <TableView
                            aria-label="Executions table"
                            selectionMode="none"
                            renderEmptyState={renderEmptyState}
                        >
                            <TableHeader>
                                <Column>Executable ID</Column>
                                <Column>Started At</Column>
                                <Column>Ended At</Column>
                                <Column>Duration</Column>
                                <Column>Status</Column>
                            </TableHeader>
                            <TableBody>
                                {(executions?.list || []).map(execution => (
                                    <Row key={execution.id}>
                                        <Cell>{execution.executable.id}</Cell>
                                        <Cell>{execution.startDate}</Cell>
                                        <Cell>{execution.endDate}</Cell>
                                        <Cell>{execution.duration}</Cell>
                                        <Cell><ExecutionStatus value={execution.status}/></Cell>
                                    </Row>
                                ))}
                            </TableBody>
                        </TableView>
                    </Item>
                </TabPanels>
            </Tabs>
        </Flex>
    );
};

export default ExecutionsPage;
