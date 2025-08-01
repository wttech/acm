import { Flex, Item, TabList, TabPanels, Tabs, Text } from '@adobe/react-spectrum';
import Beaker from '@spectrum-icons/workflow/Beaker';
import Launch from '@spectrum-icons/workflow/Launch';
import Extension from '@spectrum-icons/workflow/Extension';
import Calendar from '@spectrum-icons/workflow/Calendar';
import Hand from '@spectrum-icons/workflow/Hand';
import ScriptBootList from '../components/ScriptBootList.tsx';
import ScriptScheduleList from '../components/ScriptScheduleList.tsx';
import ScriptExtensionList from '../components/ScriptExtensionList';
import ScriptManualList from '../components/ScriptManualList';
import ScriptMockList from '../components/ScriptMockList';
import { useAppState } from '../hooks/app.ts';
import { useNavigationTab } from '../hooks/navigation';
import styles from './ScriptsPage.module.css';

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
          <Item aria-label="Boot scripts" key="boot">
            <Launch />
            <Text>Boot</Text>
          </Item>
          <Item aria-label="Schedule scripts" key="schedule">
            <Calendar/>
            <Text>Schedule</Text>
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
          <Item key="manual" aria-label="Manual Script List">
            <ScriptManualList />
          </Item>
          <Item key="boot" aria-label="Boot Script List">
            <ScriptBootList />
          </Item>
          <Item key="schedule" aria-label="Schedule Script List">
            <ScriptScheduleList />
          </Item>
          {appState.mockStatus.enabled ? (
            <Item key="mock" aria-label="Mock">
              <ScriptMockList />
            </Item>
          ) : null}
          <Item key="extension" aria-label="Extension">
            <ScriptExtensionList />
          </Item>
        </TabPanels>
      </Tabs>
    </Flex>
  );
};

export default ScriptsPage;
