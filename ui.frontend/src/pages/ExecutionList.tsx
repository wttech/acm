import React, { useEffect, useState } from 'react';
import {
    Cell,
    Column,
    Content,
    DatePicker,
    Flex,
    IllustratedMessage,
    Item,
    Picker,
    ProgressBar,
    Row,
    TableBody,
    TableHeader,
    TableView,
    View,
    Text,
} from "@adobe/react-spectrum";
import { DateValue } from '@internationalized/date';
import {ExecutionOutput, ExecutionStatus} from '../utils/api.types';
import { toastRequest } from '../utils/api';
import NotFound from "@spectrum-icons/illustrations/NotFound";
import ExecutionStatusBadge from "../components/ExecutionStatusBadge.tsx";
import ExecutableValue from "../components/ExecutableValue.tsx";
import { Key } from "@react-types/shared";
import { useNavigate } from "react-router-dom";
import Alert from '@spectrum-icons/workflow/Alert';
import Pause from '@spectrum-icons/workflow/Pause';
import Cancel from "@spectrum-icons/workflow/Cancel";
import Checkmark from "@spectrum-icons/workflow/Checkmark";
import Star from "@spectrum-icons/workflow/Star";
import {useFormatter} from "../utils/hooks.ts";

const ExecutionList = () => {
    const navigate = useNavigate();
    const [executions, setExecutions] = useState<ExecutionOutput | null>(null);

    const [startDate, setStartDate] = useState<DateValue | null>(null);
    const [endDate, setEndDate] = useState<DateValue | null>(null);
    const [status, setStatus] = useState<string | null>('all');

    const formatter = useFormatter();

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
            <Flex flex="1" justifyContent="center" alignItems="center" height="100vh">
                <ProgressBar label="Loading..." isIndeterminate />
            </Flex>
        )
    }

    return (
        <Flex direction="column" flex="1" gap="size-400">
            <Flex direction="column" gap="size-200" marginY="size-100" >
                <View borderBottomWidth="thick"
                      borderColor="gray-300"
                      paddingBottom="size-200"
                      marginBottom="size-10">
                    <Flex direction="row" gap="size-200" alignItems="center">
                        <Picker label="Status" selectedKey={status} onSelectionChange={(key) => setStatus(String(key))}>
                            <Item textValue="All" key="all">
                                <Star size="S" />
                                <Text>All</Text>
                            </Item>
                            <Item textValue="Skipped" key={ExecutionStatus.SKIPPED}>
                                <Pause size="S" />
                                <Text>Skipped</Text>
                            </Item>
                            <Item textValue="Aborted" key={ExecutionStatus.ABORTED}>
                                <Cancel size="S" />
                                <Text>Aborted</Text>
                            </Item>
                            <Item textValue="Failed" key={ExecutionStatus.FAILED}>
                                <Alert size="S" />
                                <Text>Failed</Text>
                            </Item>
                            <Item textValue="Succeeded" key={ExecutionStatus.SUCCEEDED}>
                                <Checkmark size="S" />
                                <Text>Succeeded</Text>
                            </Item>
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
                    minHeight="calc(60vh - 101px)"
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
                                <Cell>{formatter.date(execution.startDate)}</Cell>
                                <Cell>{formatter.duration(execution.duration)}</Cell>
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
