import { Flex, Item, TabList, TabPanels, Tabs, Text } from '@adobe/react-spectrum';
import Beaker from '@spectrum-icons/workflow/Beaker';
import CloseCircle from '@spectrum-icons/workflow/CloseCircle';
import Extension from '@spectrum-icons/workflow/Extension';
import FlashOn from '@spectrum-icons/workflow/FlashOn';
import Hand from '@spectrum-icons/workflow/Hand';
import ScriptListRich from '../components/ScriptListRich.tsx';
import { useAppState } from '../hooks/app.ts';
import { useNavigationTab } from '../hooks/navigation';
import { ScriptType } from '../utils/api.types';
import styles from './ScriptsPage.module.css';
import ScriptMockList from "../components/ScriptMockList.tsx";
import ScriptExtensionList from "../components/ScriptExtensionList.tsx";

const ScriptsPage = () => {
  const appState = useAppState();
  const [selectedTab, handleTabChange] = useNavigationTab('manual');

  return (
    <Flex direction="column" flex="1" gap="size-400">
      <Tabs flex="1" aria-label="Scripts" selectedKey={selectedTab} onSelectionChange={handleTabChange}>
        <TabList UNSAFE_className={styles.scriptTabs}>
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
          {appState.mockStatus.enabled ? (
            <Item aria-label="Mock scripts" key="mock">
              <Beaker />
              <Text>Mock</Text>
            </Item>
          ) : null}
          <Item aria-label="Extension scripts" key="extension">
            <Extension />
            <Text>Extension</Text>
          </Item>
        </TabList>
        <TabPanels flex="1" UNSAFE_style={{ display: 'flex' }}>
          <Item key="manual" aria-label="Manual">
            <ScriptListRich type={ScriptType.MANUAL} />
          </Item>
          <Item key="enabled" aria-label="Enabled">
            <ScriptListRich type={ScriptType.ENABLED} />
          </Item>
          <Item key="disabled" aria-label="Disabled">
            <ScriptListRich type={ScriptType.DISABLED} />
          </Item>
          {appState.mockStatus.enabled ? (
            <Item key="mock" aria-label="Mock">
              <ScriptMockList/>
            </Item>
          ) : null}
          <Item key="extension" aria-label="Extension">
            <ScriptExtensionList/>
          </Item>
        </TabPanels>
      </Tabs>
    </Flex>
  );
};

export default ScriptsPage;
