import { Flex, Item, TabList, TabPanels, Tabs, Text } from '@adobe/react-spectrum';
import Code from '@spectrum-icons/workflow/Code';
import Heart from '@spectrum-icons/workflow/Heart';
import History from '@spectrum-icons/workflow/History';
import HealthChecker from '../components/HealthChecker';
import ExecutionHistory from '../components/ExecutionHistory';
import CodeExecutor from '../components/CodeExecutor';
import { useNavigationTab } from '../hooks/navigation';

const MaintenancePage = () => {
  const [selectedTab, setSelectedTab] = useNavigationTab('script-executor');

  return (
    <Flex direction="column" flex="1" gap="size-400">
      <Tabs flex="1" aria-label="Maintenance tabs" selectedKey={selectedTab} onSelectionChange={setSelectedTab}>
        <TabList>
          <Item key="code-executor" aria-label="Code Executor">
            <Code />
            <Text>Code Executor</Text>
          </Item>
          <Item key="health-checker" aria-label="Health Checker">
            <Heart />
            <Text>Health Checker</Text>
          </Item>
          <Item key="execution-history" aria-label="Execution History">
            <History/>
            <Text>Execution History</Text>
          </Item>
        </TabList>
        <TabPanels flex="1" UNSAFE_style={{ display: 'flex' }}>
          <Item key="code-executor" aria-label="Code Executor">
            <CodeExecutor />
          </Item>
          <Item key="health-checker" aria-label="Health Checker">
            <HealthChecker />
          </Item>
          <Item key="execution-history" aria-label="Execution History">
            <ExecutionHistory />
          </Item>
        </TabPanels>
      </Tabs>
    </Flex>
  );
};

export default MaintenancePage;
