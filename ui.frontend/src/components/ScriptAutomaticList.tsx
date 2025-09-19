import { Button, ButtonGroup, Cell, Column, Content, ContextualHelp, Flex, Heading, IllustratedMessage, ProgressBar, Row, TableBody, TableHeader, TableView, Text, View } from '@adobe/react-spectrum';
import { Key, Selection } from '@react-types/shared';
import NotFound from '@spectrum-icons/illustrations/NotFound';
import Magnify from '@spectrum-icons/workflow/Magnify';
import Settings from '@spectrum-icons/workflow/Settings';
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAppState } from '../hooks/app';
import { useFormatter } from '../hooks/formatter';
import { useScripts } from '../hooks/script';
import { instanceOsgiServiceConfigUrl, InstanceOsgiServicePid, InstanceType, isExecutionNegative, ScriptType } from '../utils/api.types';
import DateExplained from './DateExplained';
import ExecutionStatsBadge from './ExecutionStatsBadge';
import ScriptExecutorStatusLight from './ScriptExecutorStatusLight';
import ScriptsAutomaticHelpButton from './ScriptsAutomaticHelpButton';
import ExecutorBootButton from './ExecutorBootButton';
import ScriptsDeleteButton from './ScriptsDeleteButton';
import ScriptsSyncButton from './ScriptsSyncButton';
import Toggle from './Toggle';
import UserInfo from './UserInfo';

const ScriptAutomaticList: React.FC = () => {
  const appState = useAppState();
  const managementEnabled = appState.spaSettings.scriptManagementEnabled;

  const navigate = useNavigate();
  const formatter = useFormatter();

  const { scripts, loading, loadScripts } = useScripts(ScriptType.AUTOMATIC);

  const [selectedKeys, setSelectedKeys] = useState<Selection>(new Set<Key>());
  const selectedIds = (selectedKeys: Selection): string[] => {
    if (selectedKeys === 'all') {
      return scripts?.list.map((script) => script.id) || [];
    } else {
      return Array.from(selectedKeys as Set<Key>).map((key) => key.toString());
    }
  };
  const handleLoadScripts = () => {
    loadScripts();
    setSelectedKeys(new Set<Key>());
  };

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
            <ButtonGroup>
              <Toggle when={managementEnabled}>
                <ScriptsDeleteButton selectedKeys={selectedIds(selectedKeys)} onDelete={handleLoadScripts} />
                <ScriptsSyncButton selectedKeys={selectedIds(selectedKeys)} onSync={handleLoadScripts} />
              </Toggle>
            </ButtonGroup>
          </Flex>
          <Flex flex="1" justifyContent="center" alignItems="center">
            <ScriptExecutorStatusLight />
          </Flex>
          <Flex flex="1" justifyContent="end" alignItems="center">
            <ButtonGroup>
              <Button variant="negative" isDisabled={appState.instanceSettings.type === InstanceType.CLOUD_CONTAINER} onPress={() => window.open(instanceOsgiServiceConfigUrl(InstanceOsgiServicePid.SCRIPT_SCHEDULER), '_blank')}>
                <Settings />
                <Text>Configure</Text>
              </Button>
              <ExecutorBootButton />
              <ScriptsAutomaticHelpButton />
            </ButtonGroup>
          </Flex>
        </Flex>
      </View>
      <TableView
        flex="1"
        aria-label={`Script list (${ScriptType.AUTOMATIC.toLowerCase()})`}
        selectionMode={managementEnabled ? 'multiple' : 'none'}
        selectedKeys={selectedKeys}
        onSelectionChange={setSelectedKeys}
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

export default ScriptAutomaticList;
