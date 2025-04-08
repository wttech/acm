import { Button, ButtonGroup, Content, Dialog, DialogTrigger, Divider, Heading, Text } from '@adobe/react-spectrum';
import Box from '@spectrum-icons/workflow/Box';
import Cancel from '@spectrum-icons/workflow/Cancel';
import Checkmark from '@spectrum-icons/workflow/Checkmark';
import Close from '@spectrum-icons/workflow/Close';
import Code from '@spectrum-icons/workflow/Code';
import Heart from '@spectrum-icons/workflow/Heart';
import Help from '@spectrum-icons/workflow/Help';
import Replay from '@spectrum-icons/workflow/Replay';
import React from 'react';

const ScriptsAutomaticHelpButton: React.FC = () => (
  <DialogTrigger>
    <Button variant="secondary" style="fill">
      <Help />
      <Text>Help</Text>
    </Button>
    {(close) => (
      <Dialog>
        <Heading>Automatic Script Execution</Heading>
        <Divider />
        <Content>
          <p>
            <Replay size="XS" /> The executor runs periodically, considering only active scripts. Each script is compiled, and its <code>canRun</code> method is evaluated.
          </p>
          <p>
            <Checkmark size="XS" /> If <code>canRun</code> succeeds, the <code>doRun</code> method is invoked. This technique allows scripts to decide on their own when to execute based on various conditions such as time, content existence,
            instance run mode, previous executions, etc.
          </p>
          <p>
            <Cancel size="XS" /> Scripts that cannot run are skipped. Skipped executions are saved in the history only if debug mode is enabled. All other script executions are always saved for auditing purposes.
          </p>
          <p>
            <Heart size="XS" /> Script executor is active only when the instance is healthy, meaning all OSGi bundles are active, and no recent core OSGi events have occurred. Some bundles may be ignored, which may be useful to address
            known issues and still consider the instance as healthy.
          </p>
          <p>
            <Code size="XS" /> This ensures that script dependencies are met, allowing the use of custom project-specific code and classes.
          </p>
          <p>
            <Box size="XS" /> Scripts have to be provided by AEM packages to ensure proper versioning and deployment and intentionally cannot be edited in the GUI. However, scripts can be ad-hoc disabled if issues arise for safety reasons,
            such as preventing faulty scripts from damaging content.
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

export default ScriptsAutomaticHelpButton;
