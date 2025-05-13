import { Button, ButtonGroup, Content, Dialog, DialogTrigger, Divider, Heading, Text } from '@adobe/react-spectrum';
import Box from '@spectrum-icons/workflow/Box';
import Close from '@spectrum-icons/workflow/Close';
import Code from '@spectrum-icons/workflow/Code';
import Mail from '@spectrum-icons/workflow/EmailNotification';
import Help from '@spectrum-icons/workflow/Help';
import Notification from '@spectrum-icons/workflow/Messenger';
import React from 'react';

const ScriptsExtensionHelpButton: React.FC = () => (
  <DialogTrigger>
    <Button variant="secondary" style="fill">
      <Help />
      <Text>Help</Text>
    </Button>
    {(close) => (
      <Dialog>
        <Heading>Extension Scripts</Heading>
        <Divider />
        <Content>
          <p>
            <Code size="XS" /> Allows to define additional variables and utilities that can be used during script execution. This enables advanced customization and integration with project-specific logic.
          </p>
          <p>
            <Notification size="XS" /> These scripts can also handle post-execution operations, such as sending direct messages, emails, or other notifications in case of script failures. This ensures that critical issues are promptly
            addressed.
          </p>
          <p>
            <Mail size="XS" /> For example, you can configure an extension script to notify your team via Slack or Microsoft Teams when a script fails, or to trigger automated workflows for error recovery.
          </p>
          <p>
            <Box size="XS" /> Extension scripts are ideal for enterprise use cases, such as integrating with external APIs, managing complex workflows, or enforcing business rules. They provide a robust mechanism to extend the platform's
            functionality while maintaining operational reliability.
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

export default ScriptsExtensionHelpButton;
