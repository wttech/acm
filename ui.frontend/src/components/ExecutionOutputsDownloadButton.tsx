import { Button, ButtonGroup, Content, Dialog, DialogContainer, Divider, Flex, Heading, Text, View } from '@adobe/react-spectrum';
import { ToastQueue } from '@react-spectrum/toast';
import Close from '@spectrum-icons/workflow/Close';
import Download from '@spectrum-icons/workflow/Download';
import FolderArchive from '@spectrum-icons/workflow/FolderArchive';
import Print from '@spectrum-icons/workflow/Print';
import React, { useState } from 'react';
import { Execution, Output, OutputNames as OutputNames } from '../utils/api.types';
import { ToastTimeoutQuick } from '../utils/spectrum.ts';

interface ExecutionOutputsDownloadButtonProps {
  execution: Execution;
}

const ExecutionOutputsDownloadButton: React.FC<ExecutionOutputsDownloadButtonProps> = ({ execution }) => {
  const [dialogOpen, setDialogOpen] = useState(false);

  const outputs = execution.outputs || {};
  const outputEntries = Object.entries(outputs);

  const handleOpenDialog = () => {
    setDialogOpen(true);
  };

  const handleCloseDialog = () => {
    setDialogOpen(false);
  };

  const downloadFile = (url: string, filename: string) => {
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const handleDownloadSingle = (output: Output) => {
    const downloadUrl = `/apps/acm/api/execution-output.json?executionId=${encodeURIComponent(execution.id)}&name=${encodeURIComponent(output.name)}`;
    downloadFile(downloadUrl, output.downloadName || output.name);
    ToastQueue.info(`Downloading ${output.label || output.name}...`, { timeout: ToastTimeoutQuick });
    handleCloseDialog();
  };

  const handleDownloadAll = () => {
    const downloadUrl = `/apps/acm/api/execution-output.json?executionId=${encodeURIComponent(execution.id)}&name=${OutputNames.ARCHIVE}`;
    downloadFile(downloadUrl, `execution-${execution.id}-complete.zip`);
    ToastQueue.info('Downloading complete archive...', { timeout: ToastTimeoutQuick });
    handleCloseDialog();
  };

  const handleDownloadConsole = () => {
    const downloadUrl = `/apps/acm/api/execution-output.json?executionId=${encodeURIComponent(execution.id)}&name=${OutputNames.CONSOLE}`;
    downloadFile(downloadUrl, `execution-${execution.id}-console.txt`);
    ToastQueue.info('Downloading console output...', { timeout: ToastTimeoutQuick });
    handleCloseDialog();
  };

  return (
    <>
      <Button variant="secondary" onPress={handleOpenDialog}>
        <Download />
        <Text>Download</Text>
      </Button>
      <DialogContainer onDismiss={handleCloseDialog}>
        {dialogOpen && (
          <Dialog minWidth="40vw">
            <Heading>Download Outputs</Heading>
            <Divider />
            <Content>
              <Flex direction="column" gap="size-200">
                <Text>Choose which outputs to download from this execution.</Text>
                <Flex direction="column" gap="size-100">
                  <View padding="size-100" backgroundColor="gray-50" borderRadius="medium">
                    <Flex direction="row" justifyContent="space-between" alignItems="center" gap="size-200">
                      <Flex direction="column">
                        <Text UNSAFE_style={{ fontWeight: 'bold' }}>Complete Archive</Text>
                        <Text UNSAFE_style={{ fontSize: 'smaller', color: 'var(--spectrum-global-color-gray-600)' }}>
                          All outputs and console log bundled as ZIP archive
                        </Text>
                      </Flex>
                      <Button variant="accent" onPress={handleDownloadAll}>
                        <FolderArchive />
                        <Text>Download ZIP</Text>
                      </Button>
                    </Flex>
                  </View>

                  <View padding="size-100" backgroundColor="gray-50" borderRadius="medium">
                    <Flex direction="row" justifyContent="space-between" alignItems="center" gap="size-200">
                      <Flex direction="column">
                        <Text UNSAFE_style={{ fontWeight: 'bold' }}>Console Output</Text>
                        <Text UNSAFE_style={{ fontSize: 'smaller', color: 'var(--spectrum-global-color-gray-600)' }}>
                          Execution log and error messages as text file
                        </Text>
                      </Flex>
                      <Button variant="secondary" onPress={handleDownloadConsole}>
                        <Print />
                        <Text>Download TXT</Text>
                      </Button>
                    </Flex>
                  </View>

                  {outputEntries.map(([key, output]) => (
                    <View key={key} padding="size-100" backgroundColor="gray-50" borderRadius="medium">
                      <Flex direction="row" justifyContent="space-between" alignItems="center" gap="size-200">
                        <Flex direction="column">
                          <Text UNSAFE_style={{ fontWeight: 'bold' }}>{output.label || output.name}</Text>
                          {output.description && (
                            <Text UNSAFE_style={{ fontSize: 'smaller', color: 'var(--spectrum-global-color-gray-600)' }}>
                              {output.description}
                            </Text>
                          )}
                        </Flex>
                        <Button variant="secondary" onPress={() => handleDownloadSingle(output)}>
                          <Download />
                          <Text>Download</Text>
                        </Button>
                      </Flex>
                    </View>
                  ))}
                  
                  {outputEntries.length === 0 && (
                    <View padding="size-200">
                      <Text UNSAFE_style={{ fontStyle: 'italic', color: 'var(--spectrum-global-color-gray-600)' }}>
                        No additional outputs generated by this execution.
                      </Text>
                    </View>
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

export default ExecutionOutputsDownloadButton;