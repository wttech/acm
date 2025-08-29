import { Flex, Item, TabList, TabPanels, Tabs, Text } from '@adobe/react-spectrum';
import ApplicationDelivery from '@spectrum-icons/workflow/ApplicationDelivery';
import Code from '@spectrum-icons/workflow/Code';
import Heart from '@spectrum-icons/workflow/Heart';
import CodeExecutor from '../components/CodeExecutor';
import HealthChecker from '../components/HealthChecker';
import Misc from '../components/Misc';
import { useNavigationTab } from '../hooks/navigation';

const MaintenancePage = () => {
  const [selectedTab, setSelectedTab] = useNavigationTab('code-executor');

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
          <Item key="misc" aria-label="Miscellaneous">
            <ApplicationDelivery />
            <Text>Miscellaneous</Text>
          </Item>
        </TabList>
        <TabPanels flex="1" UNSAFE_style={{ display: 'flex' }}>
          <Item key="code-executor" aria-label="Code Executor">
            <CodeExecutor />
          </Item>
          <Item key="health-checker" aria-label="Health Checker">
            <HealthChecker />
          </Item>
          <Item key="misc" aria-label="Miscellaneous">
            <Misc />
          </Item>
        </TabPanels>
      </Tabs>
    </Flex>
  );
};

export default MaintenancePage;
