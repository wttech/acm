import { Button, ButtonGroup, Content, Dialog, DialogTrigger, Divider, Heading, Text } from '@adobe/react-spectrum';
import Box from '@spectrum-icons/workflow/Box';
import Cancel from '@spectrum-icons/workflow/Cancel';
import Checkmark from '@spectrum-icons/workflow/Checkmark';
import Close from '@spectrum-icons/workflow/Close';
import Code from '@spectrum-icons/workflow/Code';
import Heart from '@spectrum-icons/workflow/Heart';
import Help from '@spectrum-icons/workflow/Help';
import React from 'react';

const ScriptsManualHelpButton: React.FC = () => (
    <DialogTrigger>
        <Button variant="secondary" style="fill">
            <Help />
            <Text>Help</Text>
        </Button>
        {(close) => (
            <Dialog>
                <Heading>Manual Script Execution</Heading>
                <Divider />
                <Content>
                    <p>
                        <Checkmark size="XS" /> Execute scripts on demand to perform planned content updates, data imports, and other tasks. Changes applied on the author instance will likely need to be replicated to the publish instance.
                    </p>
                    <p>
                        <Help size="XS" /> Scripts can be either argument-less or accept arguments provided through an interactive GUI dialog, allowing users to input necessary parameters before execution.
                    </p>
                    <p>
                        <Code size="XS" /> The <code>describeRun</code> method provides a description of the script's execution, including the arguments it accepts and their validation rules.
                    </p>
                    <p>
                        <Cancel size="XS" /> Those that cannot be executed are skipped. Skipped executions are recorded in the history only if debug mode is enabled. All other executions are always recorded for auditing purposes.
                    </p>
                    <p>
                        <Heart size="XS" /> The executor is active only when the instance is healthy, meaning all OSGi bundles are active and no recent core OSGi events have occurred. Some bundles may be ignored to address known issues while still considering the instance healthy.
                    </p>
                    <p>
                        <Box size="XS" /> Scripts must be provided by AEM packages to ensure proper versioning and deployment. They cannot be edited in the GUI.
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

export default ScriptsManualHelpButton;