import {
    Flex,
    Text,
    View,
    ListView,
    Item
} from '@adobe/react-spectrum';
import Settings from '@spectrum-icons/workflow/Settings';
import Code from '@spectrum-icons/workflow/Code';
import Heart from "@spectrum-icons/workflow/Heart";

const SettingsPage = () => {

    const prefix = 'http://localhost:4502';

    return (
        <Flex direction="column" flex="1" gap="size-200">
            <View
                width={{ base: '100%', M: '50%' }}
                marginX="auto"
            >
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
    );
};

export default SettingsPage;