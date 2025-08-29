import { Button, Cell, Column, Flex, Row, TableBody, TableHeader, TableView, Text } from '@adobe/react-spectrum';
import Settings from '@spectrum-icons/workflow/Settings';

import Beaker from '@spectrum-icons/workflow/Beaker';
import Filter from '@spectrum-icons/workflow/Filter';
import History from '@spectrum-icons/workflow/History';
import Info from '@spectrum-icons/workflow/Info';
import WebPage from '@spectrum-icons/workflow/WebPage';
import Data from '@spectrum-icons/workflow/Data';

import { useAppState } from '../hooks/app';
import {instancePrefix, InstanceType} from '../utils/api.types';
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
                <WebPage size="S" />
                <Text>Spa Settings</Text>
              </Flex>
            </Cell>
            <Cell>
              <Button variant="secondary" isDisabled={isCloud} onPress={() => window.open(`${instancePrefix}/system/console/configMgr/dev.vml.es.acm.core.gui.SpaSettings`, '_blank', 'noopener')}>
                <Settings />
                <Text>Configure</Text>
              </Button>
            </Cell>
          </Row>
          <Row key="code-repository">
            <Cell>
              <Flex gap="size-100">
                <Data size="S" />
                <Text>Code Repository</Text>
              </Flex>
            </Cell>
            <Cell>
              <Button variant="secondary" isDisabled={isCloud} onPress={() => window.open(`${instancePrefix}/system/console/configMgr/dev.vml.es.acm.core.code.CodeRepository`, '_blank', 'noopener')}>
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
              <Button variant="secondary" isDisabled={isCloud} onPress={() => window.open(`${instancePrefix}/system/console/configMgr/dev.vml.es.acm.core.assist.Assistancer`, '_blank', 'noopener')}>
                <Settings />
                <Text>Configure</Text>
              </Button>
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
              <Button variant="secondary" isDisabled={isCloud} onPress={() => window.open(`${instancePrefix}/system/console/configMgr/com.vml.es.aem.acm.core.mock.MockHttpFilter`, '_blank', 'noopener')}>
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
              <Button variant="secondary" isDisabled={isCloud} onPress={() => window.open(`${instancePrefix}/system/console/configMgr/dev.vml.es.acm.core.osgi.InstanceInfo`, '_blank', 'noopener')}>
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
