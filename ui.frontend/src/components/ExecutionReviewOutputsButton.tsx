import { Button, ButtonGroup, Content, Dialog, DialogContainer, Divider, Flex, Heading, Text, View, Well } from '@adobe/react-spectrum';
import { ToastQueue } from '@react-spectrum/toast';
import Close from '@spectrum-icons/workflow/Close';
import Download from '@spectrum-icons/workflow/Download';
import FolderArchive from '@spectrum-icons/workflow/FolderArchive';
import Help from '@spectrum-icons/workflow/Help';
import Print from '@spectrum-icons/workflow/Print';
import React, { useState } from 'react';
import { Execution } from '../types/execution.ts';
import { FileOutput, Output, OutputNames, TextOutput } from '../types/output.ts';
import { ToastTimeoutQuick } from '../utils/spectrum.ts';
import Magnify from '@spectrum-icons/workflow/Magnify';
import Markdown from 'react-markdown';

interface ExecutionReviewOutputsButtonProps extends Omit<React.ComponentProps<typeof Button>, 'onPress'> {
  execution: Execution;
}

const ExecutionReviewOutputsButton: React.FC<ExecutionReviewOutputsButtonProps> = ({ execution, ...buttonProps }) => {
  const [dialogOpen, setDialogOpen] = useState(false);

  const outputs = execution.outputs || {};
  const outputValues = Object.values(outputs);
  const outputFiles = outputValues.filter((output) => output.type === 'FILE') as FileOutput[];
  const outputTexts = outputValues.filter((output) => output.type === 'TEXT') as TextOutput[];

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
        <Magnify />
        <Text>Review</Text>
      </Button>
      <DialogContainer onDismiss={handleCloseDialog}>
        {dialogOpen && (
          <Dialog minWidth="40vw" maxHeight="70vh">
            <Heading>Review Outputs</Heading>
            <Divider />
            <Content>
              <Flex direction="column" gap="size-200">
                <Flex direction="column" gap="size-100">

                 {outputTexts.map((outputText) => (
                    <View key={outputText.name} padding="size-100" backgroundColor="gray-50" borderRadius="medium">
                      <Flex direction="row" justifyContent="space-between" alignItems="center" gap="size-200">
                        <Flex direction="column">
                          <Text UNSAFE_style={{ fontWeight: 'bold' }}>{outputText.label || outputText.name}</Text>
                          {outputText.description && <Text UNSAFE_style={{ fontSize: 'smaller', color: 'var(--spectrum-global-color-gray-600)' }}>{outputText.description}</Text>}
                        </Flex>
                      </Flex>
                      <Markdown children={outputText.text} />
                    </View>
                  ))}

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
                  {outputFiles.map((outputFile) => (
                    <View key={outputFile.name} padding="size-100" backgroundColor="gray-50" borderRadius="medium">
                      <Flex direction="row" justifyContent="space-between" alignItems="center" gap="size-200">
                        <Flex direction="column">
                          <Text UNSAFE_style={{ fontWeight: 'bold' }}>{outputFile.label || outputFile.name}</Text>
                          {outputFile.description && <Text UNSAFE_style={{ fontSize: 'smaller', color: 'var(--spectrum-global-color-gray-600)' }}>{outputFile.description}</Text>}
                        </Flex>
                        <Button variant="primary" onPress={() => handleDownloadSingle(outputFile)}>
                          <Download />
                          <Text>Download</Text>
                        </Button>
                      </Flex>
                    </View>
                  ))}
                  {outputValues.length === 0 && (
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

export default ExecutionReviewOutputsButton;
