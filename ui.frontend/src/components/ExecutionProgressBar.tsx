import React from 'react';
import { ProgressBar, Meter } from '@adobe/react-spectrum';
import { Strings } from '../utils/strings.ts';
import { Execution, isExecutionPending } from '../utils/api.types.ts';

interface ExecutionProgressBarProps {
  execution: Execution | null;
  active: boolean;
}

const ExecutionProgressBar: React.FC<ExecutionProgressBarProps> = ({ execution, active }) => {
  const variant = (): 'positive' | 'informative' | 'warning' | 'critical' | undefined => {
    switch (execution?.status) {
      case 'SUCCEEDED':
        return 'positive';
      case 'SKIPPED':
        return 'warning';
      case 'ABORTED':
      case 'FAILED':
        return 'critical';
      default:
        return 'informative';
    }
  };

  return (
    <>
      {execution ? (
        active || isExecutionPending(execution.status) ? (
          <ProgressBar aria-label="Executing" showValueLabel={false} label="Executing…" isIndeterminate />
        ) : (
          <Meter aria-label="Executed" variant={variant()} showValueLabel={false} value={100} label={`${Strings.capitalize(execution.status)} after ${execution.duration} ms`} />
        )
      ) : (
        <Meter aria-label="Not executing" label="Not executing" showValueLabel={false} value={0} />
      )}
    </>
  );
};

export default ExecutionProgressBar;
