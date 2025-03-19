import { Flex, Item, TabList, TabPanels, Tabs, Text } from '@adobe/react-spectrum';
import CheckmarkCircle from '@spectrum-icons/workflow/CheckmarkCircle';
import CloseCircle from '@spectrum-icons/workflow/CloseCircle';
import ScriptList from '../components/ScriptList';
import { useNavigationTab } from '../utils/hooks/navigation';

const ScriptsPage = () => {
  const [selectedTab, handleTabChange] = useNavigationTab('/scripts', 'enabled');

  return (
    <Flex direction="column" flex="1" gap="size-400">
      <Tabs flex="1" aria-label="Scripts" selectedKey={selectedTab} onSelectionChange={handleTabChange}>
        <TabList>
          <Item aria-label="Enabled scripts" key="enabled">
            <CheckmarkCircle />
            <Text>Enabled</Text>
          </Item>
          <Item aria-label="Disabled scripts" key="disabled">
            <CloseCircle />
            <Text>Disabled</Text>
          </Item>
        </TabList>
        <TabPanels flex="1" UNSAFE_style={{ display: 'flex' }}>
          <Item key="enabled">
            <ScriptList type="enabled" />
          </Item>
          <Item key="disabled">
            <ScriptList type="disabled" />
          </Item>
        </TabPanels>
      </Tabs>
    </Flex>
  );
};

export default ScriptsPage;
