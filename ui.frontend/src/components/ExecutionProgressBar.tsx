import { Meter, ProgressBar } from '@adobe/react-spectrum';
import React, { useState } from 'react';
import { useDeepCompareEffect, useInterval } from 'react-use';
import { useFormatter } from '../hooks/formatter';
import { apiRequest } from '../utils/api';
import { Execution, ExecutionStatus, isExecutableScript, isExecutionPending, ScriptOutput, ScriptStats } from '../utils/api.types';
import { Strings } from '../utils/strings';

interface ExecutionProgressBarProps {
  execution: Execution | null;
  active?: boolean;
}

const ExecutionProgressInterval = 800;

const ExecutionProgressBar: React.FC<ExecutionProgressBarProps> = ({ execution, active }) => {
  const standardLabel = () => {
    if (execution) {
      if (isExecutionPending(execution.status)) {
        return `${Strings.capitalize(execution.status)}`;
      } else {
        return `${Strings.capitalize(execution.status)} after ${formatter.durationShort(execution.duration)}`;
      }
    }
    return 'Not executing';
  };

  const formatter = useFormatter();
  const [progress, setProgress] = useState<number | null>(null);
  const [label, setLabel] = useState<string>(standardLabel);
  const [scriptStats, setScriptStats] = useState<ScriptStats | null>(null);

  useDeepCompareEffect(() => {
    const fetchScriptStats = async () => {
      if (execution && isExecutionPending(execution.status) && isExecutableScript(execution.executable.id)) {
        try {
          const response = await apiRequest<ScriptOutput>({
            operation: 'Fetch script stats',
            url: `/apps/acm/api/script.json?id=${encodeURIComponent(execution.executable.id)}`,
            method: 'get',
          });
          const stats = response.data.data.stats.find((stat: ScriptStats) => stat.path === execution.executable.id);
          setScriptStats(stats || null);
        } catch {
          setScriptStats(null);
        }
      }
    };
    fetchScriptStats();
  }, [execution]);

  const [intervalCompleted, setIntervalCompleted] = useState(false);

  useInterval(
    () => {
      if (execution) {
        if (isExecutionPending(execution.status) && scriptStats) {
          const { averageDuration } = scriptStats;
          if (execution.startDate && averageDuration) {
            const elapsedTime = formatter.durationTillNow(execution.startDate)!;
            const percentage = Math.min((elapsedTime / averageDuration) * 100, 100);

            setProgress(percentage);
            setLabel(percentage >= 100 ? `${Strings.capitalize(execution.status)} - Almost done` : `${Strings.capitalize(execution.status)} - ${formatter.durationShort(averageDuration - elapsedTime)} left`);
          } else {
            setProgress(null);
            setLabel(`${Strings.capitalize(execution.status)} - Stay tuned`);
          }
        } else {
          setProgress(null);
          setLabel(standardLabel);
          if (!isExecutionPending(execution.status)) {
            setIntervalCompleted(true);
          }
        }
      }
    },
    !intervalCompleted ? ExecutionProgressInterval : null,
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
    return <Meter width="size-3000" aria-label={label} label={label} showValueLabel={false} value={0} />;
  }

  if (active || isExecutionPending(execution.status)) {
    if (progress !== null && progress < 100) {
      return <ProgressBar minWidth="size-3000" aria-label={label} label={label} value={progress}/>;
    }
    return <ProgressBar minWidth="size-3000" aria-label={label} showValueLabel={false} label={label} isIndeterminate />;
  }

  return <Meter minWidth="size-3000" aria-label={label} variant={variant} showValueLabel={false} value={100} label={label} />;
};

export default ExecutionProgressBar;
