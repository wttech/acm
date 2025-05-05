import { Meter, ProgressBar } from '@adobe/react-spectrum';
import React, { useEffect, useState } from 'react';
import { useFormatter } from '../hooks/formatter.ts';
import { apiRequest } from '../utils/api';
import { Execution, ExecutionStatus, isExecutableScript, isExecutionActive, isExecutionCompleted, isExecutionPending, ScriptOutput } from '../utils/api.types.ts';
import { Strings } from '../utils/strings.ts';

interface ExecutionProgressBarProps {
  execution: Execution | null;
  active?: boolean;
}

const ExecutionProgressBar: React.FC<ExecutionProgressBarProps> = ({ execution, active }) => {
  const formatter = useFormatter();
  const [progress, setProgress] = useState<number | null>(null);
  const [label, setLabel] = useState<string>('Not executing');

  useEffect(() => {
    if (execution && isExecutableScript(execution.executable.id)) {
      const fetchStats = async () => {
        try {
          const response = await apiRequest<ScriptOutput>({
            operation: 'Fetch script stats',
            url: `/apps/acm/api/script.json?id=${encodeURIComponent(execution.executable.id)}`,
            method: 'get',
          });
          const output = response.data.data;
          const stats = output.stats.find((stat) => stat.path === execution.executable.id);

          if (stats?.averageDuration && execution.startDate) {
            const elapsedTime = formatter.durationTillNow(execution.startDate)!;
            const percentage = Math.min((elapsedTime / stats.averageDuration) * 100, 100);

            setProgress(percentage);

            if (isExecutionPending(execution.status) || isExecutionActive(execution.status)) {
              setLabel(percentage >= 100 ? 'Running - Almost done' : `Running - ${formatter.durationShort(stats.averageDuration - elapsedTime)} remaining`);
            } else if (isExecutionCompleted(execution.status)) {
              setLabel(Strings.capitalize(execution.status));
            }
          } else {
            setProgress(null);
            setLabel(isExecutionCompleted(execution.status) ? Strings.capitalize(execution.status) : 'Running - Stay tuned');
          }
        } catch (error) {
          console.error('Failed to fetch script stats:', error);
          setProgress(null);
          setLabel(isExecutionCompleted(execution.status) ? Strings.capitalize(execution.status) : 'Running - Stay tuned');
        }
      };

      fetchStats();
    }
  }, [execution]);

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
    if (progress !== null) {
      return <ProgressBar aria-label={label} label={label} value={progress} showValueLabel={false} />;
    }
    return <ProgressBar aria-label={label} showValueLabel={false} label={label} isIndeterminate />;
  }
  return <Meter aria-label={label} variant={variant} showValueLabel={false} value={100} label={`${label} after ${formatter.durationShort(execution.duration)}`} />;
};

export default ExecutionProgressBar;
