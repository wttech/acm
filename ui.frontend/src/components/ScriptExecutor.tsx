import React from 'react';
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
    Text, IllustratedMessage, Content
} from '@adobe/react-spectrum';
import ScriptIcon from '@spectrum-icons/workflow/Code';
import Settings from "@spectrum-icons/workflow/Settings";
import {Execution} from '../utils/api.types';
import ExecutableValue from "../components/ExecutableValue.tsx";
import { isProduction } from "../utils/node";
import NoSearchResults from "@spectrum-icons/illustrations/NoSearchResults";
import ExecutionStatusBadge from "./ExecutionStatusBadge.tsx";

const ScriptExecutor = () => {
    const prefix = isProduction() ? '' : 'http://localhost:4502';

    const queuedExecutions: Execution[] = [];

    const renderEmptyState = () => (
        <IllustratedMessage>
            <NoSearchResults/>
            <Content>No executions</Content>
        </IllustratedMessage>
    );

    return (
        <View
            backgroundColor="gray-50"
            borderWidth="thin"
            borderColor="dark"
            borderRadius="medium"
            paddingY="size-100"
            paddingX="size-200"
        >
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
                        <StatusLight variant={queuedExecutions.length === 0 ? 'positive' : 'info'}>
                            {queuedExecutions.length === 0 ? <>Idle</> : <>Busy &mdash; {queuedExecutions.length} execution(s)</>}
                        </StatusLight>
                    </Flex>
                    <Flex flex="1" justifyContent="end" alignItems="center">&nbsp;</Flex>
                </Flex>
            </View>

            <TableView aria-label="Queued Executions" renderEmptyState={renderEmptyState} selectionMode="none" marginY="size-200" minHeight="size-3400">
                <TableHeader>
                    <Column width="1fr">#</Column>
                    <Column width="12fr">Executable</Column>
                    <Column width="3fr">Status</Column>
                </TableHeader>
                <TableBody>
                    {queuedExecutions.map((execution, index) => (
                        <Row key={execution.id}>
                            <Cell>{index + 1}</Cell>
                            <Cell><ExecutableValue value={execution.executable} /></Cell>
                            <Cell><ExecutionStatusBadge value={execution.status}/></Cell>
                        </Row>
                    ))}
                </TableBody>
            </TableView>
        </View>
    );
};

export default ScriptExecutor;