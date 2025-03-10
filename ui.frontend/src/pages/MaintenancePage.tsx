import React from 'react';
import {Flex, Item, Tabs, TabList, TabPanels, Text} from '@adobe/react-spectrum';
import User from "@spectrum-icons/workflow/User";
import Data from "@spectrum-icons/workflow/Data";
import { useNavigationTab } from '../utils/hooks/navigation';
import HealthChecker from '../components/HealthChecker';
import ScriptExecutor from '../components/ScriptExecutor';
import UserPreferencesConsole from '../components/UserPreferencesConsole';

const MaintenancePage = () => {
    const [selectedTab, setSelectedTab] = useNavigationTab('/maintenance', 'instance-configuration');

    return (
        <Flex direction="column" flex="1" gap="size-200">
            <Tabs
                aria-label="Maintenance tabs"
                selectedKey={selectedTab}
                onSelectionChange={setSelectedTab}
            >
                <TabList>
                    <Item key="instance-configuration">
                        <Data />
                        <Text>Instance Configuration</Text>
                    </Item>
                    <Item key="user-preferences">
                        <User />
                        <Text>User Preferences</Text>
                    </Item>
                </TabList>
                <TabPanels>
                    <Item key="instance-configuration">
                        <Flex direction="column" flex="1" gap="size-100" marginY="size-100">
                            <ScriptExecutor />
                            <HealthChecker />
                        </Flex>
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