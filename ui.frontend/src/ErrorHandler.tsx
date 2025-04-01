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

const ErrorHandler: React.FC = () => {
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
                <Heading>Oops! Something went wrong</Heading>
                <Content>
                    <Flex gap="size-250" direction="column">
                        <Text alignSelf="start">
                            Application encountered a problem.<br/>
                            Ensure that AEM instance backend is available.
                        </Text>
                        <Disclosure>
                            <DisclosureTitle>
                                <Bug/>&nbsp;<Text>Error Details</Text>
                            </DisclosureTitle>
                            <DisclosurePanel>
                                <pre style={{overflow: 'scroll'}}>
                                {error?.stack || error?.message || 'An unknown error occurred.'}
                                </pre>
                            </DisclosurePanel>
                        </Disclosure>
                    </Flex>
                </Content>
            </IllustratedMessage>
        </Provider>
    );
};

export default ErrorHandler;