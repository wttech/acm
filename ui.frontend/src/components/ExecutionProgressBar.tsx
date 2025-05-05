import { Meter, ProgressBar } from '@adobe/react-spectrum';
import React, { useState } from 'react';
import { useInterval } from 'react-use';
import { useFormatter } from '../hooks/formatter';
import { useScriptStats } from '../hooks/script';
import { Execution, ExecutionStatus, isExecutionActive, isExecutionCompleted, isExecutionPending } from '../utils/api.types';
import { Strings } from '../utils/strings';

interface ExecutionProgressBarProps {
  execution: Execution | null;
  active?: boolean;
}

const ExecutionProgressBar: React.FC<ExecutionProgressBarProps> = ({ execution, active }) => {
  // TODO now it's laggy; only pending execution should ask for stats, etc

  const formatter = useFormatter();
  const stats = useScriptStats(execution?.executable.id || null);
  const [progress, setProgress] = useState<number | null>(null);
  const [label, setLabel] = useState<string>('Not executing');

  useInterval(
    () => {
      if (execution && stats) {
        if (execution.startDate && stats.averageDuration) {
          const elapsedTime = formatter.durationTillNow(execution.startDate)!;
          const percentage = Math.min((elapsedTime / stats.averageDuration) * 100, 100);

          setProgress(percentage);
          if (isExecutionPending(execution.status) || isExecutionActive(execution.status)) {
            setLabel(percentage >= 100 ? 'Running - Almost done' : `Running - ${formatter.durationShort(stats.averageDuration - elapsedTime)} remaining`);
          } else if (isExecutionCompleted(execution.status)) {
            setLabel(`${Strings.capitalize(execution.status)} after ${formatter.durationShort(execution.duration)}`);
          }
        } else {
          setProgress(null);
          setLabel(isExecutionCompleted(execution.status) ? Strings.capitalize(execution.status) : 'Running - Stay tuned');
        }
      }
    },
    execution && stats ? 1000 : null,
  );

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

  if (!execution) {
    return <Meter aria-label={label} label={label} showValueLabel={false} value={0} />;
  }

  if (active || isExecutionPending(execution.status)) {
    if (progress !== null && progress < 100) {
      return <ProgressBar aria-label={label} label={label} value={progress} showValueLabel={false} />;
    }
    return <ProgressBar aria-label={label} showValueLabel={false} label={label} isIndeterminate />;
  }

  return <Meter aria-label={label} variant={variant} showValueLabel={false} value={100} label={label} />;
};

export default ExecutionProgressBar;
