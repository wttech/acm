import React, {useContext} from 'react';
import {
    Button,
    Flex,
    StatusLight,
    View,
    TableView,
    TableBody,
    TableHeader,
    Column,
    Cell,
    Row,
    Text, IllustratedMessage, Content, ButtonGroup
} from '@adobe/react-spectrum';
import Clock from "@spectrum-icons/workflow/Clock";
import ExecutableValue from "../components/ExecutableValue";
import { isProduction } from "../utils/node";
import NoSearchResults from "@spectrum-icons/illustrations/NoSearchResults";
import ExecutionStatusBadge from "./ExecutionStatusBadge";
import DateExplained from "./DateExplained";
import {AppContext} from "../AppContext.tsx";
import {useNavigate} from "react-router-dom";
import {Key} from "@react-types/shared";
import ApplicationDelivery from "@spectrum-icons/workflow/ApplicationDelivery";

const ScriptExecutor = () => {
    const prefix = isProduction() ? '' : 'http://localhost:4502';
    const navigate = useNavigate();

    const context = useContext(AppContext);
    const executions = context?.queuedExecutions || [];

    const renderEmptyState = () => (
        <IllustratedMessage>
            <NoSearchResults/>
            <Content>No executions</Content>
        </IllustratedMessage>
    );

    return (
        <Flex direction="column" flex="1" gap="size-200" marginY="size-100">
            <View>
                <Flex direction="row" justifyContent="space-between" alignItems="center">
                    <Flex flex="1" alignItems="center">
                        <ButtonGroup>
                            <Button
                                variant="negative"
                                onPress={() => window.open(`${prefix}/system/console/configMgr/com.wttech.aem.contentor.core.script.ScriptExecutor`, '_blank')}
                            >
                                <ApplicationDelivery />
                                <Text>Engine</Text>
                            </Button>
                            <Button
                                variant="secondary"
                                onPress={() => window.open(`${prefix}/system/console/configMgr/org.apache.sling.event.jobs.QueueConfiguration~contentorexecutionqueue`, '_blank')}
                            >
                                <Clock />
                                <Text>Queue</Text>
                            </Button>
                        </ButtonGroup>
                    </Flex>
                    <Flex flex="1" justifyContent="center" alignItems="center">
                        <StatusLight variant={executions.length === 0 ? 'positive' : 'notice'}>
                            {executions.length === 0 ? <>Idle</> : <>Busy &mdash; {executions.length} execution(s)</>}
                        </StatusLight>
                    </Flex>
                    <Flex flex="1" justifyContent="end" alignItems="center">&nbsp;</Flex>
                </Flex>
            </View>
            <TableView flex="1" aria-label="Queued Executions" renderEmptyState={renderEmptyState} selectionMode="none" marginY="size-200" minHeight="size-3400" onAction={(key: Key) => navigate(`/executions/view/${encodeURIComponent(key)}`)}>
                <TableHeader>
                    <Column width="5%">#</Column>
                    <Column>Executable</Column>
                    <Column>Started</Column>
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
        </Flex>
    );
};

export default ScriptExecutor;