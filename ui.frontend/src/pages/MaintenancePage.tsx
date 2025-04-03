import { Flex, Item, TabList, TabPanels, Tabs, Text } from '@adobe/react-spectrum';
import Code from '@spectrum-icons/workflow/Code';
import Heart from '@spectrum-icons/workflow/Heart';
import HealthChecker from '../components/HealthChecker';
import ScriptExecutor from '../components/ScriptExecutor';
import { useNavigationTab } from '../hooks/navigation';

const MaintenancePage = () => {
  const [selectedTab, setSelectedTab] = useNavigationTab('script-executor');

  return (
    <Flex direction="column" flex="1" gap="size-400">
      <Tabs flex="1" aria-label="Maintenance tabs" selectedKey={selectedTab} onSelectionChange={setSelectedTab}>
        <TabList>
          <Item key="script-executor">
            <Code />
            <Text>Script Executor</Text>
          </Item>
          <Item key="health-checker">
            <Heart />
            <Text>Health Checker</Text>
          </Item>
        </TabList>
        <TabPanels flex="1" UNSAFE_style={{ display: 'flex' }}>
          <Item key="script-executor">
            <ScriptExecutor />
          </Item>
          <Item key="health-checker">
            <HealthChecker />
          </Item>
        </TabPanels>
      </Tabs>
    </Flex>
  );
};

export default MaintenancePage;
