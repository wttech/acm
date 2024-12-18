import React, { useEffect, useState } from 'react';
import {
    Cell,
    Column,
    Content,
    DatePicker,
    Flex,
    IllustratedMessage,
    Item,
    ProgressBar,
    Row,
    TableBody,
    TableHeader,
    TableView,
    TabList,
    TabPanels,
    Tabs,
    Text,
    View
} from "@adobe/react-spectrum";
import { DateValue } from '@internationalized/date';
import { ExecutionOutput } from '../utils/api.types';
import { toastRequest } from '../utils/api';
import NotFound from "@spectrum-icons/illustrations/NotFound";
import Folder from "@spectrum-icons/workflow/Folder";
import ExecutionStatusBadge from "../components/ExecutionStatusBadge.tsx";
import { Strings } from "../utils/strings.ts";
import ExecutableValue from "../components/ExecutableValue.tsx";
import { Key } from "@react-types/shared";
import { useNavigate } from "react-router-dom";

const ExecutionList = () => {
    const navigate = useNavigate();
    const [executions, setExecutions] = useState<ExecutionOutput | null>(null);
    const [startDate, setStartDate] = useState<DateValue | null>(null);
    const [endDate, setEndDate] = useState<DateValue | null>(null);

    useEffect(() => {
        const fetchExecutions = async () => {
            try {
                let url = `/apps/contentor/api/execution.json`;
                if (startDate || endDate) {
                    const params = new URLSearchParams();
                    if (startDate) params.append('startDate', startDate.toDate('UTC').toISOString());
                    if (endDate) params.append('endDate', endDate.toDate('UTC').toISOString());
                    url += `?${params.toString()}`;
                }
                const response = await toastRequest<ExecutionOutput>({
                    method: 'GET',
                    url,
                    operation: `Executions loading`,
                    positive: false
                });
                setExecutions(response.data.data);
            } catch (error) {
                console.error('Error fetching executions:', error);
            }
        };
        fetchExecutions();
    }, [startDate, endDate]);

    const renderEmptyState = () => (
        <IllustratedMessage>
            <NotFound />
            <Content>No executions found</Content>
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
                    <Item aria-label="All" key="all">
                        <Folder/>
                        <Text>All</Text>
                    </Item>
                </TabList>
                <TabPanels>
                    <Item key="all">
                        <Flex direction="column" gap="size-200" marginY="size-100">
                            <View>
                                <Flex direction="row" gap="size-200" alignItems="center">
                                    <DatePicker
                                        label="Start Date"
                                        value={startDate}
                                        onChange={setStartDate}
                                    />
                                    <DatePicker
                                        label="End Date"
                                        value={endDate}
                                        onChange={setEndDate}
                                    />
                                </Flex>
                            </View>
                            <TableView
                                aria-label="Executions table"
                                selectionMode="none"
                                renderEmptyState={renderEmptyState}
                                minHeight="60vh"
                                onAction={(key: Key) => navigate(`/executions/view/${encodeURIComponent(key)}`)}
                            >
                                <TableHeader>
                                    <Column>Executable</Column>
                                    <Column>Started</Column>
                                    <Column>Duration</Column>
                                    <Column>Status</Column>
                                </TableHeader>
                                <TableBody>
                                    {(executions?.list || []).map(execution => (
                                        <Row key={execution.id}>
                                            <Cell><ExecutableValue value={execution.executable}/></Cell>
                                            <Cell>{Strings.dateRelative(execution.startDate)}</Cell>
                                            <Cell>{Strings.duration(execution.duration)}</Cell>
                                            <Cell><ExecutionStatusBadge value={execution.status}/></Cell>
                                        </Row>
                                    ))}
                                </TableBody>
                            </TableView>
                        </Flex>
                    </Item>
                </TabPanels>
            </Tabs>
        </Flex>
    );
};

export default ExecutionList;
