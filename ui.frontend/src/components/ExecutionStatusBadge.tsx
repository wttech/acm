import { Badge, ProgressCircle, SpectrumBadgeProps, Text } from '@adobe/react-spectrum';
import Alert from '@spectrum-icons/workflow/Alert';
import Cancel from '@spectrum-icons/workflow/Cancel';
import Checkmark from '@spectrum-icons/workflow/Checkmark';
import Clock from '@spectrum-icons/workflow/Clock';
import Pause from '@spectrum-icons/workflow/Pause';
import React from 'react';
import { ExecutionStatus } from '../types/execution';

type ExecutionStatusProps = {
  value: ExecutionStatus;
} & Partial<SpectrumBadgeProps>;

const getVariant = (status: ExecutionStatus): 'positive' | 'negative' | 'neutral' | 'info' | 'yellow' => {
  switch (status) {
    case ExecutionStatus.SUCCEEDED:
      return 'positive';
    case ExecutionStatus.FAILED:
      return 'negative';
    case ExecutionStatus.ACTIVE:
    case ExecutionStatus.PARSING:
    case ExecutionStatus.CHECKING:
    case ExecutionStatus.RUNNING:
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
    case ExecutionStatus.PARSING:
    case ExecutionStatus.CHECKING:
    case ExecutionStatus.RUNNING:
      return <ProgressCircle size="S" aria-label="Loading…" isIndeterminate marginX="size-100" />;
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

const ExecutionStatusBadge: React.FC<ExecutionStatusProps> = ({ value, ...props }) => {
  const variant = getVariant(value);
  const icon = getIcon(value);

  return (
    <Badge variant={variant} {...props}>
      <Text>{value.toLowerCase()}</Text>
      {icon}
    </Badge>
  );
};

export default ExecutionStatusBadge;
