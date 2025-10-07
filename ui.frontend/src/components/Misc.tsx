import { Button, ButtonGroup, Cell, Column, Flex, Item, Menu, MenuTrigger, Row, TableBody, TableHeader, TableView, Text } from '@adobe/react-spectrum';
import Settings from '@spectrum-icons/workflow/Settings';

import AppRefresh from '@spectrum-icons/workflow/AppRefresh';
import Beaker from '@spectrum-icons/workflow/Beaker';
import Bell from '@spectrum-icons/workflow/Bell';
import Chat from '@spectrum-icons/workflow/Chat';
import FileCode from '@spectrum-icons/workflow/FileCode';
import Filter from '@spectrum-icons/workflow/Filter';
import History from '@spectrum-icons/workflow/History';
import Info from '@spectrum-icons/workflow/Info';
import UserGroup from '@spectrum-icons/workflow/UserGroup';

import { useAppState } from '../hooks/app';
import { instanceOsgiServiceConfigUrl, InstanceOsgiServicePid, InstanceType } from '../types/aem';
import ExecutionHistoryClearButton from './ExecutionHistoryClearButton';

const Misc = () => {
  const appState = useAppState();
  const isCloud = appState.instanceSettings.type === InstanceType.CLOUD_CONTAINER;

  return (
    <Flex direction="column" flex="1" gap="size-200" marginY="size-100">
      <TableView width="50%" aria-label="Miscellaneous Actions" selectionMode="none">
        <TableHeader>
          <Column>Service</Column>
          <Column>Action</Column>
        </TableHeader>
        <TableBody>
          <Row key="execution-history">
            <Cell>
              <Flex gap="size-100">
                <History size="S" />
                <Text>Execution History</Text>
              </Flex>
            </Cell>
            <Cell>
              <ExecutionHistoryClearButton />
            </Cell>
          </Row>
          <Row key="spa-settings">
            <Cell>
              <Flex gap="size-100">
                <AppRefresh size="S" />
                <Text>Spa Settings</Text>
              </Flex>
            </Cell>
            <Cell>
              <Button variant="secondary" isDisabled={isCloud} onPress={() => window.open(instanceOsgiServiceConfigUrl(InstanceOsgiServicePid.SPA_SETTINGS), '_blank', 'noopener')}>
                <Settings />
                <Text>Configure</Text>
              </Button>
            </Cell>
          </Row>
          <Row key="code-repository">
            <Cell>
              <Flex gap="size-100">
                <FileCode size="S" />
                <Text>Code Repository</Text>
              </Flex>
            </Cell>
            <Cell>
              <Button variant="secondary" isDisabled={isCloud} onPress={() => window.open(instanceOsgiServiceConfigUrl(InstanceOsgiServicePid.CODE_REPOSITORY), '_blank', 'noopener')}>
                <Settings />
                <Text>Configure</Text>
              </Button>
            </Cell>
          </Row>
          <Row key="code-assistancer">
            <Cell>
              <Flex gap="size-100">
                <Beaker size="S" />
                <Text>Code Assistancer</Text>
              </Flex>
            </Cell>
            <Cell>
              <Button variant="secondary" isDisabled={isCloud} onPress={() => window.open(instanceOsgiServiceConfigUrl(InstanceOsgiServicePid.CODE_ASSISTANCER), '_blank', 'noopener')}>
                <Settings />
                <Text>Configure</Text>
              </Button>
            </Cell>
          </Row>
          <Row key="notifier">
            <Cell>
              <Flex gap="size-100">
                <Bell size="S" />
                <Text>Notification</Text>
              </Flex>
            </Cell>
            <Cell>
              <ButtonGroup>
                <MenuTrigger>
                  <Button variant="secondary" isDisabled={isCloud}>
                    <Settings />
                    <Text>Configure</Text>
                  </Button>
                  <Menu onAction={(pid) => window.open(instanceOsgiServiceConfigUrl(pid as InstanceOsgiServicePid), '_blank', 'noopener')}>
                    <Item key={InstanceOsgiServicePid.NOTIFICATION_SLACK_FACTORY}>
                      <Chat />
                      <Text>Slack Notifier</Text>
                    </Item>
                    <Item key={InstanceOsgiServicePid.NOTIFICATION_TEAMS_FACTORY}>
                      <UserGroup />
                      <Text>Teams Notifier</Text>
                    </Item>
                  </Menu>
                </MenuTrigger>
              </ButtonGroup>
            </Cell>
          </Row>
          <Row key="mock-http-filter">
            <Cell>
              <Flex gap="size-100">
                <Filter size="S" />
                <Text>Mock HTTP Filter</Text>
              </Flex>
            </Cell>
            <Cell>
              <Button variant="secondary" isDisabled={isCloud} onPress={() => window.open(instanceOsgiServiceConfigUrl(InstanceOsgiServicePid.MOCK_HTTP_FILTER), '_blank', 'noopener')}>
                <Settings />
                <Text>Configure</Text>
              </Button>
            </Cell>
          </Row>
          <Row key="instance-info">
            <Cell>
              <Flex gap="size-100">
                <Info size="S" />
                <Text>Instance Info</Text>
              </Flex>
            </Cell>
            <Cell>
              <Button variant="secondary" isDisabled={isCloud} onPress={() => window.open(instanceOsgiServiceConfigUrl(InstanceOsgiServicePid.INSTANCE_INFO), '_blank', 'noopener')}>
                <Settings />
                <Text>Configure</Text>
              </Button>
            </Cell>
          </Row>
        </TableBody>
      </TableView>
    </Flex>
  );
};

export default Misc;
