import { Button, Cell, Column, Content, ContextualHelp, Flex, Heading, IllustratedMessage, Link, ProgressBar, Row, StatusLight, TableBody, TableHeader, TableView, Text, View } from '@adobe/react-spectrum';
import NotFound from '@spectrum-icons/illustrations/NotFound';
import Magnify from '@spectrum-icons/workflow/Magnify';
import { useNavigate } from 'react-router-dom';
import { useAppState } from '../hooks/app';
import { useFormatter } from '../hooks/formatter';
import { isExecutionNegative, ScriptType } from '../utils/api.types';
import DateExplained from './DateExplained';
import ExecutionStatsBadge from './ExecutionStatsBadge';
import ScriptsManualHelpButton from './ScriptsManualHelpButton';
import UserInfo from './UserInfo';
import React from "react";
import {useScripts} from "../hooks/script";

const ScriptManualList: React.FC = () => {
  const type = ScriptType.MANUAL;
  const {scripts, loading} = useScripts(type);
  const appState = useAppState();
  const navigate = useNavigate();
  const formatter = useFormatter();

  const renderEmptyState = () => (
    <IllustratedMessage>
      <NotFound />
      <Content>No scripts found</Content>
    </IllustratedMessage>
  );

  if (scripts === null || loading) {
    return (
      <Flex flex="1" justifyContent="center" alignItems="center">
        <ProgressBar label="Loading..." isIndeterminate />
      </Flex>
    );
  }

  return (
    <Flex direction="column" flex="1" gap="size-200" marginY="size-100">
      <View>
        <Flex direction="row" justifyContent="space-between" alignItems="center">
          <Flex flex="1" alignItems="center">
            &nbsp;
          </Flex>
          <Flex flex="1" justifyContent="center" alignItems="center">
            <StatusLight variant={appState.healthStatus.healthy ? 'positive' : 'negative'}>
              {appState.healthStatus.healthy ? (
                <Text>Executor active</Text>
              ) : (
                <>
                  <Text>Executor paused</Text>
                  <Text>&nbsp;&mdash;&nbsp;</Text>
                  <Link isQuiet onPress={() => navigate('/maintenance?tab=health-checker')}>
                    See health issues
                  </Link>
                </>
              )}
            </StatusLight>
          </Flex>
          <Flex flex="1" justifyContent="end" alignItems="center">
            <ScriptsManualHelpButton />
          </Flex>
        </Flex>
      </View>
      <TableView
        flex="1"
        aria-label={`Script list (${type})`}
        selectionMode={'none'}
        renderEmptyState={renderEmptyState}
        onAction={(key) => navigate(`/scripts/view/${encodeURIComponent(key)}`)}
      >
        <TableHeader>
          <Column width="4fr">Name</Column>
          <Column width="5fr">Last Execution</Column>
          <Column width="3fr">
            <Text>Average Duration</Text>
            <ContextualHelp variant="info">
              <Heading>Explanation</Heading>
              <Content>Duration is calculated based on the last {appState.spaSettings.scriptStatsLimit} completed executions (only succeeded or failed).</Content>
            </ContextualHelp>
          </Column>
          <Column width="2fr" align="end">
            <Text>Success Rate</Text>
            <ContextualHelp variant="info">
              <Heading>Explanation</Heading>
              <Content>Success rate is calculated based on the last {appState.spaSettings.scriptStatsLimit} completed executions (only succeeded or failed).</Content>
            </ContextualHelp>
          </Column>
        </TableHeader>
        <TableBody>
          {(scripts.list || []).map((script) => {
            const scriptStats = scripts.stats.find((stat) => stat.path === script.id)!;
            const lastExecution = scriptStats?.lastExecution;

            return (
              <Row key={script.id}>
                <Cell>{script.name}</Cell>
                <Cell>
                  <Flex alignItems="center" gap="size-100">
                    {lastExecution ? (
                      <>
                        <Button variant={isExecutionNegative(lastExecution.status) ? 'negative' : 'secondary'} onPress={() => navigate(`/executions/view/${encodeURIComponent(lastExecution.id)}`)} aria-label="View Execution">
                          <Magnify />
                        </Button>
                        <Text>
                          <DateExplained value={lastExecution.startDate} />
                        </Text>
                        <Text>by</Text>
                        <UserInfo id={lastExecution.userId} />
                      </>
                    ) : (
                      <Text>&mdash;</Text>
                    )}
                  </Flex>
                </Cell>
                <Cell>
                  <Text>{lastExecution ? formatter.duration(scriptStats.averageDuration) : <>&mdash;</>}</Text>
                </Cell>
                <Cell>
                  <ExecutionStatsBadge stats={scriptStats} />
                </Cell>
              </Row>
            );
          })}
        </TableBody>
      </TableView>
    </Flex>
  );
};

export default ScriptManualList;
