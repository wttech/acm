import React from 'react';
import {Flex, Item, Tabs, TabList, TabPanels, Text} from '@adobe/react-spectrum';
import User from "@spectrum-icons/workflow/User";
import Code from "@spectrum-icons/workflow/Code";
import { useNavigationTab } from '../utils/hooks/navigation';
import HealthChecker from '../components/HealthChecker';
import ScriptExecutor from '../components/ScriptExecutor';
import UserPreferencesConsole from '../components/UserPreferencesConsole';
import Heart from "@spectrum-icons/workflow/Heart";

const MaintenancePage = () => {
    const [selectedTab, setSelectedTab] = useNavigationTab('/maintenance', 'instance-configuration');

    return (
        <Flex direction="column" flex="1" gap="size-400">
            <Tabs flex="1" aria-label="Maintenance tabs" selectedKey={selectedTab} onSelectionChange={setSelectedTab}>
                <TabList>
                    <Item key="script-executor">
                        <Code/>
                        <Text>Script Executor</Text>
                    </Item>
                    <Item key="health-checker">
                        <Heart/>
                        <Text>Health Checker</Text>
                    </Item>
                    <Item key="user-preferences">
                        <User />
                        <Text>User Preferences</Text>
                    </Item>
                </TabList>
                <TabPanels flex="1" UNSAFE_style={{ display: 'flex' }}>
                    <Item key="script-executor">
                        <ScriptExecutor />
                    </Item>
                    <Item key="health-checker">
                        <HealthChecker />
                    </Item>
                    <Item key="user-preferences">
                        <Flex direction="column" flex="1" gap="size-100" marginY="size-100">
                            <UserPreferencesConsole />
                        </Flex>
                    </Item>
                </TabPanels>
            </Tabs>
        </Flex>
    );
};

export default MaintenancePage;