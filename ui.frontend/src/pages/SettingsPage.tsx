import {
    Flex,
    Text,
    View,
    ListView,
    Item,
    Checkbox,
    Tabs,
    TabList,
    TabPanels, Heading, Content
} from '@adobe/react-spectrum';
import Code from '@spectrum-icons/workflow/Code';
import Heart from "@spectrum-icons/workflow/Heart";
import User from "@spectrum-icons/workflow/User";
import Data from "@spectrum-icons/workflow/Data";
import {isProduction} from "../utils/node.ts";

const SettingsPage = () => {

    const prefix = isProduction() ? '' : 'http://localhost:4502';

    return (
        <Flex direction="column" flex="1" gap="size-200">
            <Tabs aria-label="Settings Tabs">
                <TabList>
                    <Item key="user-preferences">
                        <User />
                        <Text>User Preferences</Text>
                    </Item>
                    <Item key="instance-configuration">
                        <Data />
                        <Text>Instance Configuration</Text>
                    </Item>
                </TabList>
                <TabPanels>
                    <Item key="user-preferences">
                        <Flex direction="column" flex="1" gap="size-100" marginY="size-100">
                            <View
                                backgroundColor="gray-50"
                                borderWidth="thin"
                                borderColor="dark"
                                borderRadius="medium"
                                paddingY="size-100"
                                paddingX="size-200"
                            >
                                <Heading level={3}>Console</Heading>
                                <Content>
                                    <Checkbox isDisabled={true} isSelected={true}>Save executions</Checkbox>
                                </Content>
                            </View>
                        </Flex>
                    </Item>
                    <Item key="instance-configuration">
                        <Flex direction="column" flex="1" gap="size-100" marginY="size-100">
                            <View
                                backgroundColor="gray-50"
                                borderWidth="thin"
                                borderColor="dark"
                                borderRadius="medium"
                                paddingY="size-100"
                                paddingX="size-200"
                            >
                                <Heading level={3}>OSGi Configurations</Heading>
                                <ListView aria-label="Links" selectionMode="none">
                                    <Item textValue="Health Checker" href={`${prefix}/system/console/configMgr/com.wttech.aem.contentor.core.instance.HealthChecker`} target="_blank" rel="noopener noreferrer">
                                        <Flex alignItems="center" gap="size-100">
                                            <Heart />
                                            <Text>Health Checker</Text>
                                        </Flex>
                                    </Item>
                                    <Item textValue="Script Executor" href={`${prefix}/system/console/configMgr/com.wttech.aem.contentor.core.script.ScriptExecutor`} target="_blank" rel="noopener noreferrer">
                                        <Flex alignItems="center" gap="size-100">
                                            <Code />
                                            <Text>Script Executor</Text>
                                        </Flex>
                                    </Item>
                                </ListView>
                            </View>
                        </Flex>
                    </Item>
                </TabPanels>
            </Tabs>
        </Flex>
    );
};

export default SettingsPage;