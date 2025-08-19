import { Button, ButtonGroup, Content, Dialog, DialogTrigger, Divider, Flex, Heading, Text, View } from '@adobe/react-spectrum';
import Alert from '@spectrum-icons/workflow/Alert';
import ArchiveRemove from '@spectrum-icons/workflow/ArchiveRemove';
import Close from '@spectrum-icons/workflow/Close';
import Data from '@spectrum-icons/workflow/Data';
import Help from '@spectrum-icons/workflow/Help';
import ExecutionHistoryClearButton from './ExecutionHistoryClearButton';

const ExecutionHistory = () => {
  return (
    <Flex direction="column" flex="1" gap="size-200" marginY="size-100">
      <View>
        <Flex direction="row" justifyContent="space-between" alignItems="center">
          <Flex flex="1" alignItems="center">
            <ExecutionHistoryClearButton />
          </Flex>
          <Flex flex="1" justifyContent="center" alignItems="center">
            &nbsp;
          </Flex>
          <Flex flex="1" justifyContent="end" alignItems="center">
            <DialogTrigger>
              <Button variant="secondary" style="fill">
                <Help />
                <Text>Help</Text>
              </Button>
              {(close) => (
                <Dialog>
                  <Heading>Execution History</Heading>
                  <Divider />
                  <Content>
                    <p>
                      <ArchiveRemove size="XS" /> All executions are automatically saved in the repository for future reference and auditing.
                    </p>
                    <p>
                      <Data size="XS" /> Over time, the history may grow and consume repository space. Clearing the execution history can help free up space and maintain optimal repository performance.
                    </p>
                    <p>
                      <ArchiveRemove size="XS" /> Use the "Clear" button to remove all past executions for all instances. This action cannot be undone.
                    </p>
                    <p>
                      <Alert size="XS" /> <b>Do not clear execution history too often</b> &mdash; it may be unexpectedly useful for auditing or troubleshooting in the future.
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
          </Flex>
        </Flex>
      </View>
    </Flex>
  );
};

export default ExecutionHistory;
