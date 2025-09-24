import { Button, ButtonGroup, Content, Dialog, DialogContainer, Divider, Flex, Heading, Text, View } from '@adobe/react-spectrum';
import { ToastQueue } from '@react-spectrum/toast';
import Close from '@spectrum-icons/workflow/Close';
import Download from '@spectrum-icons/workflow/Download';
import FolderArchive from '@spectrum-icons/workflow/FolderArchive';
import React, { useState } from 'react';
import { Execution, Output } from '../utils/api.types';
import { ToastTimeoutQuick } from '../utils/spectrum.ts';

interface ExecutionOutputsDownloadButtonProps {
  execution: Execution;
}

const ExecutionOutputsDownloadButton: React.FC<ExecutionOutputsDownloadButtonProps> = ({ execution }) => {
  const [dialogOpen, setDialogOpen] = useState(false);

  const outputs = execution.outputs || {};
  const outputEntries = Object.entries(outputs);
  const hasOutputs = outputEntries.length > 0;

  const handleOpenDialog = () => {
    if (!hasOutputs) {
      ToastQueue.negative('No outputs available for download!', { timeout: ToastTimeoutQuick });
      return;
    }
    setDialogOpen(true);
  };

  const handleCloseDialog = () => {
    setDialogOpen(false);
  };

  const handleDownloadSingle = (output: Output) => {
    // TODO: Implement single file download
    // This would typically download the file using the execution ID and output name
    const downloadUrl = `/apps/acm/api/executions/${encodeURIComponent(execution.id)}/outputs/${encodeURIComponent(output.name)}/download`;
    
    // Create a temporary anchor element to trigger download
    const link = document.createElement('a');
    link.href = downloadUrl;
    link.download = output.downloadName || output.name;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    ToastQueue.info(`Downloading ${output.label || output.name}...`, { timeout: ToastTimeoutQuick });
    handleCloseDialog();
  };

  const handleDownloadAll = () => {
    // TODO: Implement ZIP download of all outputs
    const downloadUrl = `/apps/acm/api/executions/${encodeURIComponent(execution.id)}/outputs/download-all`;
    
    // Create a temporary anchor element to trigger download
    const link = document.createElement('a');
    link.href = downloadUrl;
    link.download = `execution-${execution.id}-outputs.zip`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    ToastQueue.info('Downloading all outputs as ZIP...', { timeout: ToastTimeoutQuick });
    handleCloseDialog();
  };

  return (
    <>
      <Button variant="secondary" isDisabled={!hasOutputs} onPress={handleOpenDialog}>
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
                <Text>Select an output to download, or download all outputs as a ZIP archive.</Text>
                {hasOutputs ? (
                  <Flex direction="column" gap="size-100">
                    <View padding="size-100" backgroundColor="gray-100" borderRadius="medium">
                      <Button variant="accent" onPress={handleDownloadAll} width="100%">
                        <FolderArchive />
                        <Text>Download All as ZIP</Text>
                      </Button>
                    </View>
                    {outputEntries.map(([key, output]) => (
                      <View key={key} padding="size-100" backgroundColor="gray-50" borderRadius="medium">
                        <Flex direction="column" gap="size-50">
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
                            <Text>Download &quot;{output.downloadName || output.name}&quot;</Text>
                          </Button>
                        </Flex>
                      </View>
                    ))}
                  </Flex>
                ) : (
                  <Flex justifyContent="center" alignItems="center" minHeight="size-1200">
                    <Text>No outputs available for download.</Text>
                  </Flex>
                )}
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