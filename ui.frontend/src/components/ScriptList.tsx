import {
  Button,
  ButtonGroup,
  Cell,
  Column,
  Content,
  ContextualHelp,
  Dialog,
  DialogTrigger,
  Divider,
  Flex,
  Heading,
  IllustratedMessage,
  Link,
  ProgressBar,
  Row,
  StatusLight,
  TableBody,
  TableHeader,
  TableView,
  Text,
  View
} from '@adobe/react-spectrum';
import {Key, Selection} from '@react-types/shared';
import NotFound from '@spectrum-icons/illustrations/NotFound';
import Cancel from '@spectrum-icons/workflow/Cancel';
import Checkmark from '@spectrum-icons/workflow/Checkmark';
import React, {useCallback, useContext, useEffect, useState} from 'react';
import {toastRequest} from '../utils/api';
import {ExecutionStatus, isExecutionNegative, ScriptOutput} from '../utils/api.types';
import ExecutionStatsBadge from './ExecutionStatsBadge';
import {useNavigate} from "react-router-dom";
import Magnify from "@spectrum-icons/workflow/Magnify";
import DateExplained from "./DateExplained.tsx";
import {AppContext} from "../AppContext.tsx";
import Help from "@spectrum-icons/workflow/Help";
import Replay from "@spectrum-icons/workflow/Replay";
import Heart from "@spectrum-icons/workflow/Heart";
import Code from "@spectrum-icons/workflow/Code";
import Close from "@spectrum-icons/workflow/Close";
import Box from "@spectrum-icons/workflow/Box";
import ScriptSynchronizeButton from './ScriptSynchronizeButton';
import ScriptToggleButton from './ScriptToggleButton';

type ScriptListProps = {
  type: 'enabled' | 'disabled';
};

const ScriptList: React.FC<ScriptListProps> = ({ type }) => {
  const appContext = useContext(AppContext);
  const navigate = useNavigate();

  const [scripts, setScripts] = useState<ScriptOutput | null>(null);
  const [selectedKeys, setSelectedKeys] = useState<Selection>(new Set<Key>());

  const loadScripts = useCallback(() => {
    toastRequest<ScriptOutput>({
      method: 'GET',
      url: `/apps/contentor/api/script.json?type=${type}`,
      operation: `Scripts loading (${type})`,
      positive: false,
    })
        .then((data) => setScripts(data.data.data))
        .catch((error) => console.error(`Scripts loading (${type}) error:`, error));
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

  if (scripts === null) {
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
                <ScriptToggleButton type={type} selectedKeys={selectedIds(selectedKeys)} onToggle={loadScripts} />
                <ScriptSynchronizeButton selectedKeys={selectedIds(selectedKeys)} onSync={loadScripts} />
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
                          <Link isQuiet onPress={() => navigate('/maintenance/health-checker')}>See health issues</Link>
                        </>
                    )}

                  </StatusLight>
              )}
            </Flex>
            <Flex flex="1" justifyContent="end" alignItems="center">
              <DialogTrigger>
                <Button variant="secondary" style="fill">
                  <Help />
                  <Text>Help</Text>
                </Button>
                {(close) => (
                    <Dialog>
                      <Heading>Script Execution</Heading>
                      <Divider />
                      <Content>
                        <p>
                          <Replay size="XS" /> The executor runs periodically, considering only active scripts. Each script is compiled, and its <code>canRun</code> method is evaluated.
                        </p>
                        <p>
                          <Checkmark size="XS" /> If <code>canRun</code> succeeds, the <code>doRun</code> method is invoked. This technique allows scripts to decide on their own when to execute based on various conditions such as time, content existence, instance run mode, previous executions, etc.
                        </p>
                        <p>
                          <Cancel size="XS" /> Scripts that cannot run are marked and skipped. Skipped executions are saved in the history only if debug mode is enabled. All other script executions are always saved for auditing purposes.
                        </p>
                        <p>
                          <Heart size="XS" /> Script executor is active only when the instance is healthy, meaning all OSGi bundles are active, and no recent core OSGi events have occurred. Some bundles may be enforced, requiring their deployment for the instance to be considered healthy.
                        </p>
                        <p>
                          <Code size="XS" /> This ensures that script dependencies are met, allowing the use of custom project-specific code and classes.
                        </p>
                        <p>
                          <Box size="XS" /> Scripts have to be provided by AEM packages to ensure proper versioning and deployment and intentionally cannot be edited in the GUI. However, scripts can be ad-hoc disabled if issues arise for safety reasons, such as preventing faulty scripts from damaging content.
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
        <TableView flex="1"
                   aria-label="Scripts list"
                   selectionMode="multiple"
                   selectedKeys={selectedKeys}
                   onSelectionChange={setSelectedKeys}
                   renderEmptyState={renderEmptyState}
                   onAction={(key) => {navigate(`/scripts/view/${encodeURIComponent(key)}`)}}
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
              const scriptStats = (scripts.stats.find((stat) => stat.path === script.id))!;
              const lastExecution = scriptStats?.lastExecution;

              return (
                  <Row key={script.id}>
                    <Cell>{script.name}</Cell>
                    <Cell>
                      <Flex justifyContent="space-between" alignItems="center">
                        <Text>{lastExecution ? <DateExplained value={lastExecution.startDate}/> : '—'}</Text>
                        {lastExecution && (
                            <Button variant={isExecutionNegative(lastExecution.status) ? 'negative' : 'secondary'} onPress={() => navigate(`/executions/view/${encodeURIComponent(lastExecution.id)}`)} aria-label="View Execution"><Magnify /></Button>
                        )}
                      </Flex>
                    </Cell>
                    <Cell><ExecutionStatsBadge stats={scriptStats} /></Cell>
                  </Row>
              );
            })}
          </TableBody>
        </TableView>
      </Flex>
  );
};

export default ScriptList;