import { Flex, Item, TabList, TabPanels, Tabs, Text } from '@adobe/react-spectrum';
import CloseCircle from '@spectrum-icons/workflow/CloseCircle';
import FlashOn from '@spectrum-icons/workflow/FlashOn';
import Hand from '@spectrum-icons/workflow/Hand';
import Extension from '@spectrum-icons/workflow/Extension';
import ScriptList from '../components/ScriptList';
import { useNavigationTab } from '../hooks/navigation';

const ScriptsPage = () => {
  const [selectedTab, handleTabChange] = useNavigationTab('manual');

  return (
    <Flex direction="column" flex="1" gap="size-400">
      <Tabs flex="1" aria-label="Scripts" selectedKey={selectedTab} onSelectionChange={handleTabChange}>
        <TabList>
          <Item aria-label="Manual scripts" key="manual">
            <Hand />
            <Text>Manual</Text>
          </Item>
          <Item aria-label="Automatic scripts" key="enabled">
            <FlashOn />
            <Text>Automatic</Text>
          </Item>
          <Item aria-label="Disabled scripts" key="disabled">
            <CloseCircle />
            <Text>Disabled</Text>
          </Item>
          <Item aria-label="Extension scripts" key="extension">
            <Extension />
            <Text>Extension</Text>
          </Item>
        </TabList>
        <TabPanels flex="1" UNSAFE_style={{ display: 'flex' }}>
          <Item key="manual">
            <ScriptList type="manual" />
          </Item>
          <Item key="enabled">
            <ScriptList type="enabled" />
          </Item>
          <Item key="disabled">
            <ScriptList type="disabled" />
          </Item>
          <Item key="extension">
            <ScriptList type="extension" />
          </Item>
        </TabPanels>
      </Tabs>
    </Flex>
  );
};

export default ScriptsPage;
