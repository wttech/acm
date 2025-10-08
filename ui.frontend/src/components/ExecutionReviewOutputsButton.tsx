import { Button, ButtonGroup, Content, Dialog, DialogContainer, Divider, Flex, Heading, Item, TabList, TabPanels, Tabs, Text, View, Well } from '@adobe/react-spectrum';
import { ToastQueue } from '@react-spectrum/toast';
import Close from '@spectrum-icons/workflow/Close';
import Download from '@spectrum-icons/workflow/Download';
import FileFolder from '@spectrum-icons/workflow/FileFolder';
import FolderArchive from '@spectrum-icons/workflow/FolderArchive';
import Help from '@spectrum-icons/workflow/Help';
import Info from '@spectrum-icons/workflow/Info';
import Preview from '@spectrum-icons/workflow/Preview';
import Print from '@spectrum-icons/workflow/Print';
import React, { useState } from 'react';
import { Execution } from '../types/execution.ts';
import { FileOutput, Output, OutputNames, TextOutput } from '../types/output.ts';
import { ToastTimeoutQuick } from '../utils/spectrum.ts';
import { Strings } from '../utils/strings.ts';
import Markdown from './Markdown.tsx';
import CodeTextarea from './CodeTextarea.tsx';

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
        <Preview />
        <Text>Review</Text>
      </Button>
      <DialogContainer onDismiss={handleCloseDialog}>
        {dialogOpen && (
          <Dialog minWidth="40vw" maxHeight="70vh">
            <Heading>Review Outputs</Heading>
            <Divider />
            <Content>
              <Tabs aria-label="Outputs">
                <TabList>
                  {[
                    ...(outputTexts.length > 0
                      ? [
                          <Item key="output-texts">
                            <Info size="XS" />
                            <Text>Texts</Text>
                          </Item>,
                        ]
                      : []),
                    <Item key="output-files">
                      <FileFolder size="XS" />
                      <Text>Files</Text>
                    </Item>,
                  ]}
                </TabList>
                <TabPanels>
                  {[
                    ...(outputTexts.length > 0
                      ? [
                          <Item key="output-texts">
                            <Flex marginY="size-100" direction="column" gap="size-100">
                              {outputTexts.map((outputText) => (
                                <View key={outputText.name} padding="size-100" backgroundColor="gray-50" borderRadius="medium">
                                    <Flex direction="column" gap="size-50">
                                      <Text UNSAFE_style={{ fontWeight: 'bold' }}>{outputText.label || Strings.capitalizeWords(outputText.name)}</Text>
                                      {outputText.description && <Text UNSAFE_style={{ fontSize: 'smaller', color: 'var(--spectrum-global-color-gray-600)' }}>{outputText.description}</Text>}
                                      {outputText.language ? (
                                        <CodeTextarea aria-label={`Output '${outputText.name}'`} language={outputText.language} value={outputText.value} options={{ readOnly: true, scrollBeyondLastLine: false }} />
                                      ) : (
                                        <Markdown code={outputText.value} />
                                      )}
                                    </Flex>
                                </View>
                              ))}
                            </Flex>
                          </Item>,
                        ]
                      : []),
                    <Item key="output-files">
                      <Flex marginY="size-100" direction="column" gap="size-100">
                        <View padding="size-100" backgroundColor="gray-50" borderRadius="medium">
                          <Flex direction="row" justifyContent="space-between" alignItems="center" gap="size-200">
                            <Flex direction="column" gap="size-50">
                              <Text UNSAFE_style={{ fontWeight: 'bold' }}>Archive</Text>
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
                            <Flex direction="column" gap="size-50">
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
                              <Flex direction="column" gap="size-50">
                                <Text UNSAFE_style={{ fontWeight: 'bold' }}>{outputFile.label || Strings.capitalizeWords(outputFile.name)}</Text>
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
                    </Item>,
                  ]}
                </TabPanels>
              </Tabs>
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
