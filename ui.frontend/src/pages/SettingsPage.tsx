import React, {useContext} from 'react';
import {
    Button,
    Checkbox,
    Content,
    Flex,
    Heading,
    Item,
    ListView,
    StatusLight,
    TabList,
    TabPanels,
    Tabs,
    Text,
    View,
    TableView,
    TableBody,
    TableHeader,
    Badge,
    Column, Cell, Row
} from '@adobe/react-spectrum';
import Code from '@spectrum-icons/workflow/Code';
import ScriptIcon from '@spectrum-icons/workflow/Code';
import HeartIcon from "@spectrum-icons/workflow/Heart";
import User from "@spectrum-icons/workflow/User";
import Data from "@spectrum-icons/workflow/Data";
import Settings from "@spectrum-icons/workflow/Settings";
import {isProduction} from "../utils/node";
import {useNavigationTab} from '../utils/hooks/navigation';
import {AppContext} from '../AppContext';
import {Execution, ExecutionStatus, HealthIssueSeverity} from '../utils/api.types';
import { IconColorValue } from '@react-types/shared';

const SettingsPage = () => {
    const [selectedTab, setSelectedTab] = useNavigationTab('/settings', 'instance-configuration');
    const prefix = isProduction() ? '' : 'http://localhost:4502';
    const context = useContext(AppContext);

    const healthIssues = context?.healthStatus.issues || [];
    const queuedExecutions: Execution[] = [
        { id: '1', executable: { id: '1', content: 'Backup Script' }, status: ExecutionStatus.QUEUED, startDate: '', endDate: '', duration: 0, output: '', error: null },
        { id: '2', executable: { id: '2', content: 'Cleanup Script' }, status: ExecutionStatus.QUEUED, startDate: '', endDate: '', duration: 0, output: '', error: null }
    ];

    return (
        <Flex direction="column" flex="1" gap="size-200">
            <Tabs
                aria-label="Settings Tabs"
                selectedKey={selectedTab}
                onSelectionChange={setSelectedTab}
            >
                <TabList>
                    <Item key="instance-configuration">
                        <Data />
                        <Text>Instance Configuration</Text>
                    </Item>
                    <Item key="user-preferences">
                        <User />
                        <Text>User Preferences</Text>
                    </Item>
                </TabList>
                <TabPanels>
                    <Item key="instance-configuration">
                        <Flex direction="column" flex="1" gap="size-100" marginY="size-100">
                            <View
                                backgroundColor="gray-50"
                                borderWidth="thin"
                                borderColor="dark"
                                borderRadius="medium"
                                paddingY="size-100"
                                paddingX="size-200"
                            >
                                <Flex alignItems="center" gap="size-100">
                                    <HeartIcon />
                                    <Heading level={3}>Health Checker</Heading>
                                </Flex>
                                <Flex alignItems="center" gap="size-100">
                                    <Button
                                        variant="cta"
                                        onPress={() => window.open(`${prefix}/system/console/configMgr/com.wttech.aem.contentor.core.instance.HealthChecker`, '_blank')}
                                    >
                                        <Settings />
                                        <Text>Configure</Text>
                                    </Button>
                                    <StatusLight variant={healthIssues.length === 0 ? 'positive' : 'negative'}>
                                        {healthIssues.length === 0 ? <>Healthy &mdash; 0 issue(s)</> : <>Unhealthy &mdash; {healthIssues.length} issue(s)</>}
                                    </StatusLight>
                                </Flex>
                                {healthIssues.length > 0 && (
                                    <TableView aria-label="Health Issues" selectionMode="none" marginY="size-200">
                                        <TableHeader>
                                            <Column width="0.1fr">#</Column>
                                            <Column width="0.2fr">Severity</Column>
                                            <Column>Message</Column>
                                        </TableHeader>
                                        <TableBody>
                                            {healthIssues.map((issue, index) => (
                                                <Row key={index}>
                                                    <Cell>{index + 1}</Cell>
                                                    <Cell>
                                                        <Badge variant={getSeverityColor(issue.severity)}>
                                                            {issue.severity}
                                                        </Badge>
                                                    </Cell>
                                                    <Cell>{issue.message}</Cell>
                                                </Row>
                                            ))}
                                        </TableBody>
                                    </TableView>
                                )}
                            </View>
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
                                <Flex alignItems="center" gap="size-100">
                                    <Button
                                        variant="cta"
                                        onPress={() => window.open(`${prefix}/system/console/configMgr/com.wttech.aem.contentor.core.script.ScriptExecutor`, '_blank')}
                                    >
                                        <Settings />
                                        <Text>Configure</Text>
                                    </Button>
                                    <StatusLight variant={queuedExecutions.length === 0 ? 'positive' : 'info'}>
                                        {queuedExecutions.length === 0 ? <>No Executions &mdash; 0 script(s)</> : <>Queued &mdash; {queuedExecutions.length} script(s)</>}
                                    </StatusLight>
                                </Flex>
                                {queuedExecutions.length > 0 && (
                                    <ListView aria-label="Queued Executions" selectionMode="none" marginY="size-200">
                                        {queuedExecutions.map(execution => (
                                            <Item key={execution.id} textValue={execution.executable.content}>
                                                <Flex alignItems="center" gap="size-100">
                                                    <Code />
                                                    <Text>{execution.executable.content}</Text>
                                                </Flex>
                                            </Item>
                                        ))}
                                    </ListView>
                                )}
                            </View>
                        </Flex>
                    </Item>
                    <Item key="user-preferences">
                        <Flex direction="column" flex="1" gap="size-100" marginY="size-100">
                            <View
                                backgroundColor="gray-50"
                                borderWidth="thin"
                                borderColor="dark"
                                borderRadius="medium"
                                paddingY="size-100"
                                paddingX="size-200"
                            >
                                <Heading level={3}>Console</Heading>
                                <Content>
                                    <Checkbox isDisabled={true} isSelected={true}>Save executions</Checkbox>
                                </Content>
                            </View>
                        </Flex>
                    </Item>
                </TabPanels>
            </Tabs>
        </Flex>
    );
};

const getSeverityColor = (severity: HealthIssueSeverity): IconColorValue => {
    switch (severity) {
        case HealthIssueSeverity.CRITICAL:
            return 'negative';
        case HealthIssueSeverity.WARNING:
            return 'notice';
        default:
            return 'informative';
    }
};

export default SettingsPage;