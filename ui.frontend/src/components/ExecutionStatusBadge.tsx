import { Badge, Text } from '@adobe/react-spectrum';
import Alert from '@spectrum-icons/workflow/Alert';
import Cancel from '@spectrum-icons/workflow/Cancel';
import Checkmark from '@spectrum-icons/workflow/Checkmark';
import Clock from '@spectrum-icons/workflow/Clock';
import Pause from '@spectrum-icons/workflow/Pause';
import React from 'react';
import { ExecutionStatus } from '../utils/api.types';

interface ExecutionStatusProps {
  value: ExecutionStatus;
}

const getBadgeVariant = (status: ExecutionStatus): 'positive' | 'negative' | 'neutral' | 'info' | 'yellow' => {
  switch (status) {
    case ExecutionStatus.SUCCEEDED:
      return 'positive';
    case ExecutionStatus.FAILED:
      return 'negative';
    case ExecutionStatus.ACTIVE:
      return 'info';
    case ExecutionStatus.QUEUED:
      return 'yellow';
    case ExecutionStatus.STOPPED:
    case ExecutionStatus.SKIPPED:
      return 'neutral';
    case ExecutionStatus.ABORTED:
      return 'negative';
    default:
      return 'neutral';
  }
};

const getIcon = (status: ExecutionStatus) => {
  switch (status) {
    case ExecutionStatus.SUCCEEDED:
      return <Checkmark />;
    case ExecutionStatus.FAILED:
      return <Alert />;
    case ExecutionStatus.ACTIVE:
      return <Clock />;
    case ExecutionStatus.QUEUED:
      return <Clock />;
    case ExecutionStatus.STOPPED:
      return <Pause />;
    case ExecutionStatus.SKIPPED:
      return <Pause />;
    case ExecutionStatus.ABORTED:
      return <Cancel />;
    default:
      return null;
  }
};

const ExecutionStatusBadge: React.FC<ExecutionStatusProps> = ({ value }) => {
  const variant = getBadgeVariant(value);
  const icon = getIcon(value);

  return (
    <Badge variant={variant}>
      <Text>{value.toLowerCase()}</Text>
      {icon}
    </Badge>
  );
};

export default ExecutionStatusBadge;
