import { Button, ButtonGroup, Cell, Column, Content, ContextualHelp, Flex, Heading, IllustratedMessage, Link, ProgressBar, Row, StatusLight, TableBody, TableHeader, TableView, Text, View } from '@adobe/react-spectrum';
import { Key, Selection } from '@react-types/shared';
import NotFound from '@spectrum-icons/illustrations/NotFound';
import Magnify from '@spectrum-icons/workflow/Magnify';
import React, { useCallback, useContext, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AppContext } from '../AppContext.tsx';
import { toastRequest } from '../utils/api';
import { InstanceRole, isExecutionNegative, ScriptOutput, ScriptType } from '../utils/api.types';
import DateExplained from './DateExplained.tsx';
import ExecutionStatsBadge from './ExecutionStatsBadge';
import ScriptSynchronizeButton from './ScriptSynchronizeButton';
import ScriptToggleButton from './ScriptToggleButton';
import ScriptsAutomaticHelpButton from './ScriptsAutomaticHelpButton';
import ScriptsExtensionHelpButton from './ScriptsExtensionHelpButton.tsx';
import ScriptsManualHelpButton from './ScriptsManualHelpButton';

type ScriptListProps = {
  type: ScriptType;
};

const ScriptList: React.FC<ScriptListProps> = ({ type }) => {
  const appContext = useContext(AppContext);
  const navigate = useNavigate();

  const [scripts, setScripts] = useState<ScriptOutput | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [selectedKeys, setSelectedKeys] = useState<Selection>(new Set<Key>());

  const loadScripts = useCallback(() => {
    setLoading(true);
    toastRequest<ScriptOutput>({
      method: 'GET',
      url: `/apps/acm/api/script.json?type=${type}`,
      operation: `Scripts loading (${type})`,
      positive: false,
    })
      .then((data) => setScripts(data.data.data))
      .catch((error) => console.error(`Scripts loading (${type}) error:`, error))
      .finally(() => setLoading(false));
  }, [type]);

  useEffect(() => {
    loadScripts();
  }, [type, loadScripts]);

  const selectedIds = (selectedKeys: Selection): string[] => {
    if (selectedKeys === 'all') {
      return scripts?.list.map((script) => script.id) || [];
    } else {
      return Array.from(selectedKeys as Set<Key>).map((key) => key.toString());
    }
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
              {type === ScriptType.ENABLED || type === ScriptType.DISABLED ? (
                <>
                  <ScriptToggleButton type={type} selectedKeys={selectedIds(selectedKeys)} onToggle={loadScripts} />
                  {appContext && appContext.instanceSettings.role == InstanceRole.AUTHOR && <ScriptSynchronizeButton selectedKeys={selectedIds(selectedKeys)} onSync={loadScripts} />}
                </>
              ) : null}
            </ButtonGroup>
          </Flex>
          <Flex flex="1" justifyContent="center" alignItems="center">
            {appContext && (
              <StatusLight variant={appContext.healthStatus.healthy ? 'positive' : 'negative'}>
                {appContext.healthStatus.healthy ? (
                  <Text>Executor active</Text>
                ) : (
                  <>
                    <Text>Executor paused</Text>
                    <Text>&nbsp;&mdash;&nbsp;</Text>
                    <Link isQuiet onPress={() => navigate('/maintenance/health-checker')}>
                      See health issues
                    </Link>
                  </>
                )}
              </StatusLight>
            )}
          </Flex>
          <Flex flex="1" justifyContent="end" alignItems="center">
            {type === ScriptType.MANUAL ? <ScriptsManualHelpButton /> : type === ScriptType.EXTENSION ? <ScriptsExtensionHelpButton /> : <ScriptsAutomaticHelpButton />}
          </Flex>
        </Flex>
      </View>
      {type === ScriptType.EXTENSION ? (
        <TableView flex="1" aria-label={`Script list (${type})`} renderEmptyState={renderEmptyState} onAction={(key) => navigate(`/scripts/view/${encodeURIComponent(key)}`)}>
          <TableHeader>
            <Column>Name</Column>
          </TableHeader>
          <TableBody>
            {(scripts.list || []).map((script) => (
              <Row key={script.id}>
                <Cell>{script.name}</Cell>
              </Row>
            ))}
          </TableBody>
        </TableView>
      ) : (
        <TableView
          flex="1"
          aria-label={`Script list (${type})`}
          selectionMode={type === ScriptType.MANUAL ? 'none' : 'multiple'}
          selectedKeys={selectedKeys}
          onSelectionChange={setSelectedKeys}
          renderEmptyState={renderEmptyState}
          onAction={(key) => navigate(`/scripts/view/${encodeURIComponent(key)}`)}
        >
          <TableHeader>
            <Column>Name</Column>
            <Column>Last Execution</Column>
            <Column>
              Success Rate
              <ContextualHelp variant="help">
                <Heading>Explanation</Heading>
                <Content>Success rate is calculated based on the last 30 completed executions (only succeeded or failed).</Content>
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
                      {lastExecution && (
                        <Button variant={isExecutionNegative(lastExecution.status) ? 'negative' : 'secondary'} onPress={() => navigate(`/executions/view/${encodeURIComponent(lastExecution.id)}`)} aria-label="View Execution">
                          <Magnify />
                        </Button>
                      )}
                      <Text>{lastExecution ? <DateExplained value={lastExecution.startDate} /> : '—'}</Text>
                    </Flex>
                  </Cell>
                  <Cell>
                    <ExecutionStatsBadge stats={scriptStats} />
                  </Cell>
                </Row>
              );
            })}
          </TableBody>
        </TableView>
      )}
    </Flex>
  );
};

export default ScriptList;
