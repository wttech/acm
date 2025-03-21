import { Meter, ProgressBar } from '@adobe/react-spectrum';
import React from 'react';
import { Execution, ExecutionStatus, isExecutionPending } from '../utils/api.types.ts';
import { useFormatter } from '../utils/hooks/formatter.ts';
import { Strings } from '../utils/strings.ts';

interface ExecutionProgressBarProps {
  execution: Execution | null;
  active?: boolean;
}

const ExecutionProgressBar: React.FC<ExecutionProgressBarProps> = ({ execution, active }) => {
  const formatter = useFormatter();

  const variant = ((): 'positive' | 'informative' | 'warning' | 'critical' | undefined => {
    switch (execution?.status) {
      case ExecutionStatus.SUCCEEDED:
        return 'positive';
      case ExecutionStatus.SKIPPED:
        return 'warning';
      case ExecutionStatus.ABORTED:
      case ExecutionStatus.FAILED:
        return 'critical';
      default:
        return 'informative';
    }
  })();
  const label = execution ? Strings.capitalize(execution.status) : 'Not executing';

  if (!execution) {
    return <Meter aria-label={label} label={label} showValueLabel={false} value={0} />;
  }

  if (active || isExecutionPending(execution.status)) {
    return <ProgressBar aria-label={label} showValueLabel={false} label={label} isIndeterminate />;
  }

  return <Meter aria-label={label} variant={variant} showValueLabel={false} value={100} label={`${label} after ${formatter.durationShort(execution.duration)}`} />;
};

export default ExecutionProgressBar;
