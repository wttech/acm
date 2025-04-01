import React from 'react';
import { useRouteError } from 'react-router-dom';
import {
    IllustratedMessage,
    Heading,
    Content,
    defaultTheme,
    Provider,
    Text,
    Flex,
    Disclosure, DisclosureTitle, DisclosurePanel
} from '@adobe/react-spectrum';
import Error from '@spectrum-icons/illustrations/Error';
import Bug from "@spectrum-icons/workflow/Bug";

const ErrorComponent: React.FC = () => {
    const error = useRouteError() as Error;
    console.error("[ACM] Error:", error);

    return (
        <Provider
            theme={defaultTheme}
            colorScheme={'light'}
            locale={'en-uk'}
            UNSAFE_style={{
                display: 'flex',
                height: '100%',
                flexDirection: 'column',
            }}
        >
            <IllustratedMessage>
                <Error />
                <Heading>AEM Content Manager</Heading>
                <Content>
                    <Flex gap="size-250" direction="column">
                        <Text alignSelf="start">
                            Oops! Application encountered an error.<br/>
                            Ensure that AEM instance backend is available.
                        </Text>
                        <Disclosure>
                            <DisclosureTitle>
                                <Bug/>&nbsp;<Text>Error Details</Text>
                            </DisclosureTitle>
                            <DisclosurePanel>
                                {error?.stack || error?.message || 'An unknown error occurred.'}
                            </DisclosurePanel>
                        </Disclosure>
                    </Flex>
                </Content>
            </IllustratedMessage>
        </Provider>
    );
};

export default ErrorComponent;