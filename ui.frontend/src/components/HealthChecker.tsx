import React, { useContext } from 'react';
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
    Badge,
    Text,
    IllustratedMessage,
    Content,
} from '@adobe/react-spectrum';
import Settings from "@spectrum-icons/workflow/Settings";
import { AppContext } from '../AppContext';
import { HealthIssueSeverity } from '../utils/api.types';
import { isProduction } from "../utils/node.ts";
import NoSearchResults from "@spectrum-icons/illustrations/NoSearchResults";

const HealthChecker = () => {
    const context = useContext(AppContext);
    const healthIssues = context?.healthStatus.issues || [];
    const prefix = isProduction() ? '' : 'http://localhost:4502';

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

    const renderEmptyState = () => (
        <IllustratedMessage>
            <NoSearchResults/>
            <Content>No issues</Content>
        </IllustratedMessage>
    );

    return (
        <Flex direction="column" flex="1" gap="size-200" marginY="size-100">
            <View>
                <Flex direction="row" justifyContent="space-between" alignItems="center">
                    <Flex flex="1" alignItems="center">
                        <Button
                            variant="negative"
                            onPress={() => window.open(`${prefix}/system/console/configMgr/com.wttech.aem.contentor.core.instance.HealthChecker`, '_blank')}
                        >
                            <Settings />
                            <Text>Configuration</Text>
                        </Button>
                    </Flex>
                    <Flex flex="1" justifyContent="center" alignItems="center">
                        <StatusLight variant={healthIssues.length === 0 ? 'positive' : 'negative'}>
                            {healthIssues.length === 0 ? <>Healthy</> : <>Unhealthy &mdash; {healthIssues.length} issue(s)</>}
                        </StatusLight>
                    </Flex>
                    <Flex flex="1" justifyContent="end" alignItems="center">&nbsp;</Flex>
                </Flex>
            </View>

            <TableView flex="1" aria-label="Health Issues" renderEmptyState={renderEmptyState} selectionMode="none" marginY="size-200" minHeight="size-3400">
                <TableHeader>
                    <Column width="5%">#</Column>
                    <Column>Severity</Column>
                    <Column >Message</Column>
                </TableHeader>
                <TableBody>
                    {(healthIssues || []).map((issue, index) => (
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
        </Flex>
    );
};

export default HealthChecker;