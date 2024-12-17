import React, {useEffect, useState} from 'react';
import {
    Cell,
    Column,
    Content,
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
    Text
} from "@adobe/react-spectrum";
import {ExecutionOutput} from '../utils/api.types';
import {toastRequest} from '../utils/api';
import NotFound from "@spectrum-icons/illustrations/NotFound";
import Folder from "@spectrum-icons/workflow/Folder";
import ExecutionStatusBadge from "../components/ExecutionStatusBadge.tsx";
import {Strings} from "../utils/strings.ts";
import ExecutableValue from "../components/ExecutableValue.tsx";
import {Key} from "@react-types/shared";
import {useNavigate} from "react-router-dom";

const ExecutionList = () => {
    const navigate = useNavigate();
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
                        <TableView
                            aria-label="Executions table"
                            selectionMode="none"
                            renderEmptyState={renderEmptyState}
                            minHeight="60vh"
                            onAction={(key: Key) => navigate(`/executions/view/${key}`)}
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
                    </Item>
                </TabPanels>
            </Tabs>
        </Flex>
    );
};

export default ExecutionList;
