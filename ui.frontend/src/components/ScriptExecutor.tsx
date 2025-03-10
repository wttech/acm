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
    Text
} from '@adobe/react-spectrum';
import ScriptIcon from '@spectrum-icons/workflow/Code';
import Settings from "@spectrum-icons/workflow/Settings";
import {Execution, ExecutionStatus} from '../utils/api.types';
import ExecutableValue from "../components/ExecutableValue.tsx";
import { isProduction } from "../utils/node";

const ScriptExecutor = () => {
    const prefix = isProduction() ? '' : 'http://localhost:4502';

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
    );
};

export default ScriptExecutor;