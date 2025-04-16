import { Button, ButtonGroup, Cell, Column, Content, Dialog, DialogTrigger, Divider, Flex, Heading, IllustratedMessage, Item, Menu, MenuTrigger, Row, StatusLight, TableBody, TableHeader, TableView, Text, View } from '@adobe/react-spectrum';
import { Key, Selection } from '@react-types/shared';
import NoSearchResults from '@spectrum-icons/illustrations/NoSearchResults';
import ApplicationDelivery from '@spectrum-icons/workflow/ApplicationDelivery';
import Cancel from '@spectrum-icons/workflow/Cancel';
import Checkmark from '@spectrum-icons/workflow/Checkmark';
import Clock from '@spectrum-icons/workflow/Clock';
import Close from '@spectrum-icons/workflow/Close';
import Code from '@spectrum-icons/workflow/Code';
import Help from '@spectrum-icons/workflow/Help';
import Replay from '@spectrum-icons/workflow/Replay';
import Settings from '@spectrum-icons/workflow/Settings';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAppState } from '../hooks/app.ts';
import { InstanceType } from '../utils/api.types.ts';
import { isProduction } from '../utils/node';
import DateExplained from './DateExplained';
import ExecutableIdValue from './ExecutableIdValue';
import ExecutionsAbortButton from './ExecutionsAbortButton';
import ExecutionStatusBadge from './ExecutionStatusBadge';

const ScriptExecutor = () => {
  const prefix = isProduction() ? '' : 'http://localhost:4502';
  const navigate = useNavigate();

  const appState = useAppState();
  const executions = appState.queuedExecutions;

  const [selectedKeys, setSelectedKeys] = useState<Selection>(new Set<Key>());
  const selectedIds = (selectedKeys: Selection): string[] => {
    if (selectedKeys === 'all') {
      return executions?.map((execution) => execution.id) || [];
    } else {
      return Array.from(selectedKeys as Set<Key>).map((key) => key.toString());
    }
  };

  const renderEmptyState = () => (
    <IllustratedMessage>
      <NoSearchResults />
      <Content>No executions</Content>
    </IllustratedMessage>
  );

  return (
    <Flex direction="column" flex="1" gap="size-200" marginY="size-100">
      <View>
        <Flex direction="row" justifyContent="space-between" alignItems="center">
          <Flex flex="1" alignItems="center">
            <ButtonGroup>
              <ExecutionsAbortButton selectedKeys={selectedIds(selectedKeys)} />
              <MenuTrigger>
                <Button variant="negative" isDisabled={appState.instanceSettings.type === InstanceType.CLOUD_CONTAINER}>
                  <Settings />
                  <Text>Configure</Text>
                </Button>
                <Menu onAction={(pid) => window.open(`${prefix}/system/console/configMgr/${pid}`, '_blank')}>
                  <Item key="com.vml.es.aem.acm.core.script.ScriptScheduler">
                    <ApplicationDelivery />
                    <Text>Script Scheduler</Text>
                  </Item>
                  <Item key="org.apache.sling.event.jobs.QueueConfiguration~acmexecutionqueue">
                    <Clock />
                    <Text>Execution Queue</Text>
                  </Item>
                  <Item key="com.vml.es.aem.acm.core.code.Executor">
                    <Code />
                    <Text>Code Executor</Text>
                  </Item>
                </Menu>
              </MenuTrigger>
            </ButtonGroup>
          </Flex>
          <Flex flex="1" justifyContent="center" alignItems="center">
            <StatusLight variant={executions.length === 0 ? 'positive' : 'notice'}>{executions.length === 0 ? <>Idle</> : <>Busy &mdash; {executions.length} execution(s)</>}</StatusLight>
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
      <TableView
        flex="1"
        aria-label="Queued Executions"
        renderEmptyState={renderEmptyState}
        selectionMode="multiple"
        selectedKeys={selectedKeys}
        onSelectionChange={setSelectedKeys}
        marginY="size-200"
        minHeight="size-3400"
        onAction={(key: Key) => navigate(`/executions/view/${encodeURIComponent(key)}`)}
      >
        <TableHeader>
          <Column width="5%">#</Column>
          <Column>Executable</Column>
          <Column>User</Column>
          <Column>Started</Column>
          <Column>Status</Column>
        </TableHeader>
        <TableBody>
          {executions.map((execution, index) => (
            <Row key={execution.id}>
              <Cell>{index + 1}</Cell>
              <Cell>
                <ExecutableIdValue id={execution.executableId} />
              </Cell>
              <Cell>
                <Text>{execution.userId}</Text>
              </Cell>
              <Cell>
                <DateExplained value={execution.startDate} />
              </Cell>
              <Cell>
                <ExecutionStatusBadge value={execution.status} />
              </Cell>
            </Row>
          ))}
        </TableBody>
      </TableView>
    </Flex>
  );
};

export default ScriptExecutor;
