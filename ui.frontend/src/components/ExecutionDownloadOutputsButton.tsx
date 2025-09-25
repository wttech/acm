import { Button, ButtonGroup, Content, Dialog, DialogContainer, Divider, Flex, Heading, Text, View, Well } from '@adobe/react-spectrum';
import { ToastQueue } from '@react-spectrum/toast';
import Close from '@spectrum-icons/workflow/Close';
import Download from '@spectrum-icons/workflow/Download';
import FolderArchive from '@spectrum-icons/workflow/FolderArchive';
import Help from '@spectrum-icons/workflow/Help';
import Print from '@spectrum-icons/workflow/Print';
import React, { useState } from 'react';
import { Execution, Output, OutputNames } from '../utils/api.types.ts';
import { ToastTimeoutQuick } from '../utils/spectrum.ts';

interface ExecutionDownloadOutputsButtonProps extends Omit<React.ComponentProps<typeof Button>, 'onPress'> {
  execution: Execution;
}

const ExecutionDownloadOutputsButton: React.FC<ExecutionDownloadOutputsButtonProps> = ({ execution, ...buttonProps }) => {
  const [dialogOpen, setDialogOpen] = useState(false);

  const outputs = execution.outputs || {};
  const outputEntries = Object.entries(outputs);

  const handleOpenDialog = () => {
    setDialogOpen(true);
  };

  const handleCloseDialog = () => {
    setDialogOpen(false);
  };

  const downloadFile = (url: string) => {
    const link = document.createElement('a');
    link.href = url;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const handleDownloadSingle = (output: Output) => {
    const downloadUrl = `/apps/acm/api/execution-output.json?executionId=${encodeURIComponent(execution.id)}&name=${encodeURIComponent(output.name)}`;
    downloadFile(downloadUrl);
    ToastQueue.info(`Downloading ${output.label || output.name}...`, { timeout: ToastTimeoutQuick });
    handleCloseDialog();
  };

  const handleDownloadAll = () => {
    const downloadUrl = `/apps/acm/api/execution-output.json?executionId=${encodeURIComponent(execution.id)}&name=${OutputNames.ARCHIVE}`;
    downloadFile(downloadUrl);
    ToastQueue.info('Downloading complete archive...', { timeout: ToastTimeoutQuick });
    handleCloseDialog();
  };

  const handleDownloadConsole = () => {
    const downloadUrl = `/apps/acm/api/execution-output.json?executionId=${encodeURIComponent(execution.id)}&name=${OutputNames.CONSOLE}`;
    downloadFile(downloadUrl);
    ToastQueue.info('Downloading console output...', { timeout: ToastTimeoutQuick });
    handleCloseDialog();
  };

  return (
    <>
      <Button {...buttonProps} onPress={handleOpenDialog}>
        <Download />
        <Text>Download</Text>
      </Button>
      <DialogContainer onDismiss={handleCloseDialog}>
        {dialogOpen && (
          <Dialog minWidth="40vw" maxHeight="70vh">
            <Heading>Download Outputs</Heading>
            <Divider />
            <Content>
              <Flex direction="column" gap="size-200">
                <Flex direction="column" gap="size-100">
                  <View padding="size-100" backgroundColor="gray-50" borderRadius="medium">
                    <Flex direction="row" justifyContent="space-between" alignItems="center" gap="size-200">
                      <Flex direction="column">
                        <Text UNSAFE_style={{ fontWeight: 'bold' }}>Complete Archive</Text>
                        <Text UNSAFE_style={{ fontSize: 'smaller', color: 'var(--spectrum-global-color-gray-600)' }}>Console and generated outputs bundled as ZIP archive</Text>
                      </Flex>
                      <Button variant="cta" onPress={handleDownloadAll}>
                        <FolderArchive />
                        <Text>Download</Text>
                      </Button>
                    </Flex>
                  </View>

                  <View padding="size-100" backgroundColor="gray-50" borderRadius="medium">
                    <Flex direction="row" justifyContent="space-between" alignItems="center" gap="size-200">
                      <Flex direction="column">
                        <Text UNSAFE_style={{ fontWeight: 'bold' }}>Console</Text>
                        <Text UNSAFE_style={{ fontSize: 'smaller', color: 'var(--spectrum-global-color-gray-600)' }}>Execution logs and errors as text file</Text>
                      </Flex>
                      <Button variant="primary" onPress={handleDownloadConsole}>
                        <Print />
                        <Text>Download</Text>
                      </Button>
                    </Flex>
                  </View>

                  {outputEntries.map(([key, output]) => (
                    <View key={key} padding="size-100" backgroundColor="gray-50" borderRadius="medium">
                      <Flex direction="row" justifyContent="space-between" alignItems="center" gap="size-200">
                        <Flex direction="column">
                          <Text UNSAFE_style={{ fontWeight: 'bold' }}>{output.label || output.name}</Text>
                          {output.description && <Text UNSAFE_style={{ fontSize: 'smaller', color: 'var(--spectrum-global-color-gray-600)' }}>{output.description}</Text>}
                        </Flex>
                        <Button variant="primary" onPress={() => handleDownloadSingle(output)}>
                          <Download />
                          <Text>Download</Text>
                        </Button>
                      </Flex>
                    </View>
                  ))}
                  {outputEntries.length === 0 && (
                    <Well UNSAFE_style={{ padding: 'var(--spectrum-global-dimension-size-100)' }}>
                      <Text>
                        <Help size="XS" /> No additional outputs generated by this execution.
                      </Text>
                    </Well>
                  )}
                </Flex>
              </Flex>
            </Content>
            <ButtonGroup>
              <Button aria-label="Close" variant="secondary" onPress={handleCloseDialog}>
                <Close size="XS" />
                <Text>Close</Text>
              </Button>
            </ButtonGroup>
          </Dialog>
        )}
      </DialogContainer>
    </>
  );
};

export default ExecutionDownloadOutputsButton;
