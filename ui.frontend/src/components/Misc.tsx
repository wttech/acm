import { Button, Cell, Column, Flex, Row, TableBody, TableHeader, TableView, Text } from '@adobe/react-spectrum';
import Code from '@spectrum-icons/workflow/Code';
import Settings from '@spectrum-icons/workflow/Settings';
import { useAppState } from '../hooks/app';
import { InstanceType } from '../utils/api.types';
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
            <Cell>Execution History</Cell>
            <Cell>
              <ExecutionHistoryClearButton />
            </Cell>
          </Row>
          <Row key="spa-settings">
            <Cell>Spa Settings</Cell>
            <Cell>
              <Button variant="secondary" isDisabled={isCloud} onPress={() => window.open('/system/console/configMgr/dev.vml.es.acm.core.gui.SpaSettings', '_blank', 'noopener')}>
                <Settings />
                <Text>Configure</Text>
              </Button>
            </Cell>
          </Row>
          <Row key="mock-http-filter">
            <Cell>Mock HTTP Filter</Cell>
            <Cell>
              <Button variant="secondary" isDisabled={isCloud} onPress={() => window.open('/system/console/configMgr/com.vml.es.aem.acm.core.mock.MockHttpFilter', '_blank', 'noopener')}>
                <Settings />
                <Text>Configure</Text>
              </Button>
            </Cell>
          </Row>
          <Row key="instance-info">
            <Cell>
              <Flex gap="size-100">
                <Code size="S" />
                <Text>Instance Info</Text>
              </Flex>
            </Cell>
            <Cell>
              <Button variant="secondary" isDisabled={isCloud} onPress={() => window.open('/system/console/configMgr/dev.vml.es.acm.core.osgi.InstanceInfo', '_blank', 'noopener')}>
                <Settings />
                <Text>Configure</Text>
              </Button>
            </Cell>
          </Row>
          <Row key="code-repository">
            <Cell>
              <Flex gap="size-100">
                <Code size="S" />
                <Text>Code Repository</Text>
              </Flex>
            </Cell>
            <Cell>
              <Button variant="secondary" isDisabled={isCloud} onPress={() => window.open('/system/console/configMgr/dev.vml.es.acm.core.code.CodeRepository', '_blank', 'noopener')}>
                <Settings />
                <Text>Configure</Text>
              </Button>
            </Cell>
          </Row>
          <Row key="code-assistancer">
            <Cell>
              <Flex gap="size-100">
                <Code size="S" />
                <Text>Code Assistancer</Text>
              </Flex>
            </Cell>
            <Cell>
              <Button variant="secondary" isDisabled={isCloud} onPress={() => window.open('/system/console/configMgr/dev.vml.es.acm.core.assist.Assistancer', '_blank', 'noopener')}>
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
