import React, {useContext} from 'react';
import {
    Button,
    Checkbox,
    Content,
    Flex,
    Heading,
    Item,
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
    Column, Cell, Row, ButtonGroup, Link, DialogTrigger, Dialog, Divider
} from '@adobe/react-spectrum';
import ScriptIcon from '@spectrum-icons/workflow/Code';
import HeartIcon from "@spectrum-icons/workflow/Heart";
import User from "@spectrum-icons/workflow/User";
import Data from "@spectrum-icons/workflow/Data";
import Settings from "@spectrum-icons/workflow/Settings";
import {isProduction} from "../utils/node";
import {useNavigationTab} from '../utils/hooks/navigation';
import {AppContext} from '../AppContext';
import {Execution, ExecutionStatus, HealthIssueSeverity} from '../utils/api.types';
import ExecutableValue from "../components/ExecutableValue.tsx";
import ScriptToggleButton from "../components/ScriptToggleButton.tsx";
import ScriptSynchronizeButton from "../components/ScriptSynchronizeButton.tsx";
import Help from "@spectrum-icons/workflow/Help";
import Replay from "@spectrum-icons/workflow/Replay";
import Checkmark from "@spectrum-icons/workflow/Checkmark";
import Cancel from "@spectrum-icons/workflow/Cancel";
import Heart from "@spectrum-icons/workflow/Heart";
import Code from "@spectrum-icons/workflow/Code";
import Box from "@spectrum-icons/workflow/Box";
import Close from "@spectrum-icons/workflow/Close";

const SettingsPage = () => {
    const [selectedTab, setSelectedTab] = useNavigationTab('/settings', 'instance-configuration');
    const prefix = isProduction() ? '' : 'http://localhost:4502';
    const context = useContext(AppContext);

    const healthIssues = context?.healthStatus.issues || [];
    const queuedExecutions: Execution[] = [
        {
            "executable": {
                "id": "console",
                "content": "boolean canRun() {\n  return condition.always()\n}\n  \nvoid doRun() {\n  println \"Processing...\"\n  \n  println \"Updating resources...\"\n  def max = 10\n  for (int i = 0; i < max; i++) {\n    Thread.sleep(500)\n    println \"Updated (${i + 1}/${max})\"\n  }\n  \n  println \"Processing done\"\n}\n"
            },
            "id": "2025/3/7/9/8/bbb380fc-7b98-442e-9430-d5c6182a98d0_480",
            "status": "SUCCEEDED" as ExecutionStatus,
            "startDate": "2025-03-07T09:08:44.366+00:00",
            "endDate": "2025-03-07T09:08:49.557+00:00",
            "duration": 5191,
            "error": null,
            "output": "Processing...\nUpdating resources...\nUpdated (1/10)\nUpdated (2/10)\nUpdated (3/10)\nUpdated (4/10)\nUpdated (5/10)\nUpdated (6/10)\nUpdated (7/10)\nUpdated (8/10)\nUpdated (9/10)\nUpdated (10/10)\nProcessing done\n"
        },
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

                                <View>
                                    <Flex direction="row" justifyContent="space-between" alignItems="center">
                                        <Flex flex="1" alignItems="center">
                                            <Button
                                                variant="cta"
                                                onPress={() => window.open(`${prefix}/system/console/configMgr/com.wttech.aem.contentor.core.instance.HealthChecker`, '_blank')}
                                            >
                                                <Settings />
                                                <Text>Configure</Text>
                                            </Button>
                                        </Flex>
                                        <Flex flex="1" justifyContent="center" alignItems="center">
                                            <StatusLight variant={healthIssues.length === 0 ? 'positive' : 'negative'}>
                                                {healthIssues.length === 0 ? <>Healthy &mdash; 0 issue(s)</> : <>Unhealthy &mdash; {healthIssues.length} issue(s)</>}
                                            </StatusLight>
                                        </Flex>
                                        <Flex flex="1" justifyContent="end" alignItems="center">&nbsp;</Flex>
                                    </Flex>
                                </View>

                                {healthIssues.length > 0 && (
                                    <TableView aria-label="Health Issues" selectionMode="none" marginY="size-200">
                                        <TableHeader>
                                            <Column width="1fr">#</Column>
                                            <Column width="1fr">Severity</Column>
                                            <Column width="12fr">Message</Column>
                                        </TableHeader>
                                        <TableBody>
                                            {healthIssues.map((issue, index) => (
                                                <Row key={index}>
                                                    <Cell>{index + 1}</Cell>
                                                    <Cell>
                                                        <Badge variant={getSeverityVariant(issue.severity)}>{issue.severity}</Badge>
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
                                                {queuedExecutions.length === 0 ? <>No Executions</> : <>Queued &mdash; {queuedExecutions.length} execution(s)</>}
                                            </StatusLight>
                                        </Flex>
                                        <Flex flex="1" justifyContent="end" alignItems="center">&nbsp;</Flex>
                                    </Flex>
                                </View>

                                {queuedExecutions.length > 0 && (
                                    <TableView aria-label="Queued Executions" selectionMode="none" marginY="size-200">
                                        <TableHeader>
                                            <Column width="1fr">#</Column>
                                            <Column width="12fr">Name</Column>
                                        </TableHeader>
                                        <TableBody>
                                            {queuedExecutions.map((execution, index) => (
                                                <Row key={execution.id}>
                                                    <Cell>{index + 1}</Cell>
                                                    <Cell><ExecutableValue value={execution.executable} /></Cell>
                                                </Row>
                                            ))}
                                        </TableBody>
                                    </TableView>
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

const getSeverityVariant = (severity: HealthIssueSeverity): 'negative' | 'yellow' | 'neutral' => {
    switch (severity) {
        case HealthIssueSeverity.CRITICAL:
            return 'negative';
        case HealthIssueSeverity.WARNING:
            return 'yellow';
        default:
            return 'neutral';
    }
};

export default SettingsPage;