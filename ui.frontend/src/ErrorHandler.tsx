import { Content, defaultTheme, Disclosure, DisclosurePanel, DisclosureTitle, Flex, Heading, IllustratedMessage, Provider, Text } from '@adobe/react-spectrum';
import Error from '@spectrum-icons/illustrations/Error';
import Bug from '@spectrum-icons/workflow/Bug';
import React from 'react';
import { useRouteError } from 'react-router-dom';

const ErrorHandler: React.FC = () => {
  const error = useRouteError() as Error;

  console.error('[ACM] Error:', error);

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
            <Text>
              Application encountered a problem.<br/>
              Verify your authorization status.<br/>
              Ensure that AEM instance backend is available.
            </Text>
            <Disclosure>
              <DisclosureTitle>
                <Bug />
                &nbsp;<Text>Error Details</Text>
              </DisclosureTitle>
              <DisclosurePanel>
                <pre style={{ overflow: 'scroll' }}>{error?.stack || error?.message || 'An unknown error occurred.'}</pre>
              </DisclosurePanel>
            </Disclosure>
          </Flex>
        </Content>
      </IllustratedMessage>
    </Provider>
  );
};

export default ErrorHandler;
