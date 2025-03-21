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
  const variant = (): 'positive' | 'informative' | 'warning' | 'critical' | undefined => {
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
  };

  const progressBarLabel = (): string => {
    return execution ? Strings.capitalize(execution.status) + '…' : 'Executing…';
  };

  const formatter = useFormatter();

  return (
    <>
      {execution ? (
        active || isExecutionPending(execution.status) ? (
          <ProgressBar aria-label="Executing" showValueLabel={false} label={progressBarLabel()} isIndeterminate />
        ) : (
          <Meter aria-label="Executed" variant={variant()} showValueLabel={false} value={100} label={`${Strings.capitalize(execution.status)} after ${formatter.durationShort(execution.duration)}`} />
        )
      ) : (
        <Meter aria-label="Not executing" label="Not executing" showValueLabel={false} value={0} />
      )}
    </>
  );
};

export default ExecutionProgressBar;
