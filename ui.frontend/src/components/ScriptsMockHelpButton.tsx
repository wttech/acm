import { Button, ButtonGroup, Content, Dialog, DialogTrigger, Divider, Heading, Text } from '@adobe/react-spectrum';
import Box from '@spectrum-icons/workflow/Box';
import Checkmark from '@spectrum-icons/workflow/Checkmark';
import Close from '@spectrum-icons/workflow/Close';
import Help from '@spectrum-icons/workflow/Help';
import Notification from '@spectrum-icons/workflow/Messenger';
import React from 'react';

const ScriptsMockHelpButton: React.FC = () => (
  <DialogTrigger>
    <Button variant="secondary" style="fill">
      <Help />
      <Text>Help</Text>
    </Button>
    {(close) => (
      <Dialog>
        <Heading>Mock Scripts</Heading>
        <Divider />
        <Content>
          <p>
            <Checkmark size="XS" /> Dedicated to simulate responses from 3rd-party systems during development and testing. They allow you to define static or generate on-the-fly responses for specific request paths.
          </p>
          <p>
            <Box size="XS" /> These scripts are matched to requests based on the filter's configured regular expressions. Only requests that pass the filter are evaluated by the mock scripts <code>request()</code> method. If some mock
            script is matched to the request, its <code>response()</code> method is called to generate the response.
          </p>
          <p>
            <Notification size="XS" /> Mock scripts can handle various scenarios, including success responses, error cases, and edge conditions, ensuring robust testing coverage.
          </p>
        </Content>
        <ButtonGroup>
          <Button variant="secondary" onPress={close}>
            <Close size="XS" />
            <Text>Close</Text>
          </Button>
        </ButtonGroup>
      </Dialog>
    )}
  </DialogTrigger>
);

export default ScriptsMockHelpButton;
