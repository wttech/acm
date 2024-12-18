import React, { useEffect, useState } from 'react';
import {Cell, Column, Content, DatePicker, Flex, IllustratedMessage, Item, Picker, ProgressBar, Row, TableBody, TableHeader, TableView, View} from "@adobe/react-spectrum";
import { DateValue } from '@internationalized/date';
import {ExecutionOutput, ExecutionStatus, isExecutionPending} from '../utils/api.types';
import { toastRequest } from '../utils/api';
import NotFound from "@spectrum-icons/illustrations/NotFound";
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
    const [status, setStatus] = useState<string | null>('all');

    const statusOptions = Object.values(ExecutionStatus)
        .filter(status => !isExecutionPending(status))
        .map(status => ({ key: status.toLowerCase(), name: status.charAt(0) + status.slice(1).toLowerCase() }));

    useEffect(() => {
        const fetchExecutions = async () => {
            try {
                let url = `/apps/contentor/api/execution.json`;
                const params = new URLSearchParams();
                if (startDate) params.append('startDate', startDate.toString());
                if (endDate) params.append('endDate', endDate.toString());
                if (status && status !== 'all') params.append('status', status);
                if (params.toString()) url += `?${params.toString()}`;
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
    }, [startDate, endDate, status]);

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
            <Flex direction="column" gap="size-200" marginY="size-100" >
                <View borderBottomWidth="thick"
                      borderColor="gray-300"
                      paddingBottom="size-200"
                      marginBottom="size-10">
                    <Flex direction="row" gap="size-200" alignItems="center">
                        <Picker
                            label="Status"
                            selectedKey={status}
                            onSelectionChange={(key) => setStatus(String(key))}
                            items={[
                                { key: 'all', name: 'All' },
                                ...statusOptions
                            ]}
                        >
                            {(item) => <Item key={item.key}>{item.name}</Item>}
                        </Picker>
                        <DatePicker
                            label="Start Date"
                            granularity="second"
                            value={startDate}
                            onChange={setStartDate}
                        />
                        <DatePicker
                            label="End Date"
                            granularity="second"
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
                                <Cell>{Strings.dateExplained(execution.startDate)}</Cell>
                                <Cell>{Strings.duration(execution.duration)}</Cell>
                                <Cell><ExecutionStatusBadge value={execution.status}/></Cell>
                            </Row>
                        ))}
                    </TableBody>
                </TableView>
            </Flex>
        </Flex>
    );
};

export default ExecutionList;
