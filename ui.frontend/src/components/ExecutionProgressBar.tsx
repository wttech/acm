import { Meter, ProgressBar } from '@adobe/react-spectrum';
import React from 'react';
import { Execution, isExecutionPending } from '../utils/api.types.ts';
import { Strings } from '../utils/strings.ts';
import {useFormatter} from "../utils/hooks/formatter.ts";

interface ExecutionProgressBarProps {
  execution: Execution | null;
  active?: boolean;
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

  const formatter = useFormatter();

  return (
    <>
      {execution ? (
        active || isExecutionPending(execution.status) ? (
          <ProgressBar aria-label="Executing" showValueLabel={false} label="Executing…" isIndeterminate />
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
