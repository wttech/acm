import React from 'react';
import {
    View,
    Heading,
    Content,
    Checkbox
} from '@adobe/react-spectrum';

const UserPreferencesConsole: React.FC = () => {
    return (
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
    );
};

export default UserPreferencesConsole;