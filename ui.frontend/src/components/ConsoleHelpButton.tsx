import { Button, ButtonGroup, Content, Dialog, DialogTrigger, Divider, Heading, Text } from '@adobe/react-spectrum';
import Bug from '@spectrum-icons/workflow/Bug';
import Cancel from '@spectrum-icons/workflow/Cancel';
import Close from '@spectrum-icons/workflow/Close';
import Gears from '@spectrum-icons/workflow/Gears';
import Help from '@spectrum-icons/workflow/Help';
import Print from '@spectrum-icons/workflow/Print';
import Spellcheck from '@spectrum-icons/workflow/Spellcheck';

const ConsoleHelpButton = () => (
  <DialogTrigger>
    <Button variant="secondary" style="fill">
      <Help />
      <Text>Help</Text>
    </Button>
    {(close) => (
      <Dialog>
        <Heading>Code execution</Heading>
        <Divider />
        <Content>
          <p>
            <Print size="XS" /> Output is printed live.
          </p>
          <p>
            <Cancel size="XS" /> <Text>Abort if the execution:</Text>
            <ul style={{ listStyleType: 'none' }}>
              <li>
                <Spellcheck size="XS" /> is taking too long
              </li>
              <li>
                <Bug size="XS" /> is stuck in an infinite loop
              </li>
              <li>
                <Gears size="XS" /> makes the instance unresponsive
              </li>
            </ul>
          </p>
          <p>
            <Help size="XS" /> Be aware that aborting execution may leave data in an inconsistent state.
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

export default ConsoleHelpButton;
