import { Button, Text } from '@adobe/react-spectrum';
import { ToastQueue } from '@react-spectrum/toast';
import Copy from '@spectrum-icons/workflow/Copy';
import React from 'react';

const toastTimeout = 3000;

interface ExecutionCopyOutputButtonProps {
  output: string;
}

const ExecutionCopyOutputButton: React.FC<ExecutionCopyOutputButtonProps> = ({ output }) => {
  const onCopyExecutionOutput = () => {
    if (output) {
      navigator.clipboard
        .writeText(output)
        .then(() => {
          ToastQueue.info('Execution output copied to clipboard!', {
            timeout: toastTimeout,
          });
        })
        .catch(() => {
          ToastQueue.negative('Failed to copy execution output!', {
            timeout: toastTimeout,
          });
        });
    } else {
      ToastQueue.negative('No execution output to copy!', {
        timeout: toastTimeout,
      });
    }
  };

  return (
    <Button variant="secondary" isDisabled={!output} onPress={onCopyExecutionOutput}>
      <Copy />
      <Text>Copy</Text>
    </Button>
  );
};

export default ExecutionCopyOutputButton;
