import React, { useContext, useState } from 'react';
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
    Text,
    IllustratedMessage,
    Content,
    ButtonGroup,
    Dialog,
    DialogTrigger,
    Heading,
    Divider, Item, Menu, MenuTrigger
} from '@adobe/react-spectrum';
import Clock from "@spectrum-icons/workflow/Clock";
import ExecutableValue from "../components/ExecutableValue";
import { isProduction } from "../utils/node";
import NoSearchResults from "@spectrum-icons/illustrations/NoSearchResults";
import ExecutionStatusBadge from "./ExecutionStatusBadge";
import DateExplained from "./DateExplained";
import { AppContext } from "../AppContext.tsx";
import { useNavigate } from "react-router-dom";
import { Key } from "@react-types/shared";
import ApplicationDelivery from "@spectrum-icons/workflow/ApplicationDelivery";
import Help from "@spectrum-icons/workflow/Help";
import Close from "@spectrum-icons/workflow/Close";
import Replay from "@spectrum-icons/workflow/Replay";
import Checkmark from "@spectrum-icons/workflow/Checkmark";
import Cancel from "@spectrum-icons/workflow/Cancel";
import Settings from "@spectrum-icons/workflow/Settings";

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
                        <MenuTrigger>
                            <Button variant="negative">
                                <Settings />
                                <Text>Configure</Text>
                            </Button>
                            <Menu onAction={(pid) => window.open(`${prefix}/system/console/configMgr/${pid}`, '_blank')}>
                                <Item key="com.wttech.aem.contentor.core.script.ScriptExecutor"><ApplicationDelivery /><Text>Engine</Text></Item>
                                <Item key="org.apache.sling.event.jobs.QueueConfiguration~contentorexecutionqueue"><Clock/><Text>Queue</Text></Item>
                            </Menu>
                        </MenuTrigger>
                    </Flex>
                    <Flex flex="1" justifyContent="center" alignItems="center">
                        <StatusLight variant={executions.length === 0 ? 'positive' : 'notice'}>
                            {executions.length === 0 ? <>Idle</> : <>Busy &mdash; {executions.length} execution(s)</>}
                        </StatusLight>
                    </Flex>
                    <Flex flex="1" justifyContent="end" alignItems="center">
                        <DialogTrigger>
                            <Button variant="secondary" style="fill">
                                <Help />
                                <Text>Help</Text>
                            </Button>
                            {(close) => (
                                <Dialog>
                                    <Heading>Script Executor</Heading>
                                    <Divider />
                                    <Content>
                                        <p>
                                            <Replay size="XS" /> Here you can preview queued and active executions. You can also abort them if they were run in the background by other users or in separate browser tabs/windows.
                                        </p>
                                        <p>
                                            <Checkmark size="XS" /> It allows you to freely hit the &apos;Execute&apos; button in the console, close the browser, and get back to the script output anytime. Once an execution ends, it is saved in history.
                                        </p>
                                        <p>
                                            <Cancel size="XS" /> Remember that aborting executions may leave data in an inconsistent state.
                                        </p>
                                    </Content>
                                    <ButtonGroup>
                                        <Button variant="secondary" onPress={close}>
                                            <Close size="XS" />
                                            <Text>Close</Text>
                                        </Button>
                                    </ButtonGroup>
                                </Dialog>
                            )}
                        </DialogTrigger>
                    </Flex>
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