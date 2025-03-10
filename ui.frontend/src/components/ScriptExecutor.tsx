import React, { useEffect, useState } from 'react';
import {
    Button,
    Flex,
    Heading,
    StatusLight,
    View,
    TableView,
    TableBody,
    TableHeader,
    Column,
    Cell,
    Row,
    Text, IllustratedMessage, Content, ProgressBar
} from '@adobe/react-spectrum';
import ScriptIcon from '@spectrum-icons/workflow/Code';
import Settings from "@spectrum-icons/workflow/Settings";
import {Execution, QueueOutput} from '../utils/api.types';
import ExecutableValue from "../components/ExecutableValue";
import { isProduction } from "../utils/node";
import NoSearchResults from "@spectrum-icons/illustrations/NoSearchResults";
import ExecutionStatusBadge from "./ExecutionStatusBadge";
import { toastRequest } from '../utils/api';
import DateExplained from "./DateExplained";

const ScriptExecutor = () => {
    const prefix = isProduction() ? '' : 'http://localhost:4502';
    const [executions, setExecutions] = useState<Execution[]>([]);
    const [loading, setLoading] = useState<boolean>(false);

    useEffect(() => {
        const fetchExecutions = async () => {
            setLoading(true);
            try {
                const response = await toastRequest<QueueOutput>({
                    method: 'GET',
                    url: `/apps/contentor/api/queue-code.json`,
                    operation: `Queued executions loading`,
                    positive: false,
                });
                setExecutions(response.data.data.executions);
            } catch (error) {
                console.error('Cannot load queued executions:', error);
            } finally {
                setLoading(false);
            }
        };
        fetchExecutions();
    }, [prefix]);

    const renderEmptyState = () => (
        <IllustratedMessage>
            <NoSearchResults/>
            <Content>No executions</Content>
        </IllustratedMessage>
    );

    return (
        <View>
            <Flex alignItems="center" gap="size-100">
                <ScriptIcon />
                <Heading level={3}>Script Executor</Heading>
            </Flex>

            <View>
                <Flex direction="row" justifyContent="space-between" alignItems="center">
                    <Flex flex="1" alignItems="center">
                        <Button
                            variant="cta"
                            onPress={() => window.open(`${prefix}/system/console/configMgr/com.wttech.aem.contentor.core.script.ScriptExecutor`, '_blank')}
                        >
                            <Settings />
                            <Text>Configure</Text>
                        </Button>
                    </Flex>
                    <Flex flex="1" justifyContent="center" alignItems="center">
                        <StatusLight variant={executions.length === 0 ? 'positive' : 'info'}>
                            {executions.length === 0 ? <>Idle</> : <>Busy &mdash; {executions.length} execution(s)</>}
                        </StatusLight>
                    </Flex>
                    <Flex flex="1" justifyContent="end" alignItems="center">&nbsp;</Flex>
                </Flex>
            </View>

            {loading ? (
                <Flex flex="1" justifyContent="center" alignItems="center" height="100vh">
                    <ProgressBar label="Loading..." isIndeterminate />
                </Flex>
            ) : (
                <TableView aria-label="Queued Executions" renderEmptyState={renderEmptyState} selectionMode="none" marginY="size-200" minHeight="size-3400">
                    <TableHeader>
                        <Column width="5%">#</Column>
                        <Column>Executable</Column>
                        <Column>Started At</Column>
                        <Column>Status</Column>
                    </TableHeader>
                    <TableBody>
                        {executions.map((execution, index) => (
                            <Row key={execution.id}>
                                <Cell>{index + 1}</Cell>
                                <Cell><ExecutableValue value={execution.executable} /></Cell>
                                <Cell><DateExplained value={execution.startDate}/></Cell>
                                <Cell><ExecutionStatusBadge value={execution.status}/></Cell>
                            </Row>
                        ))}
                    </TableBody>
                </TableView>
            )}
        </View>
    );
};

export default ScriptExecutor;