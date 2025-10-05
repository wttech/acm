import { Badge, Text } from '@adobe/react-spectrum';
import React from 'react';
import { isExecutionCompleted } from '../types/execution';
import { ExecutionStatus } from '../types/execution';
import { ScriptStats } from '../types/script';

interface ExecutionStatsBadgeProps {
  stats: ScriptStats;
}

const getBadgeVariant = (successRate: number, totalExecutions: number): 'positive' | 'negative' | 'neutral' | 'info' | 'yellow' => {
  if (totalExecutions === 0) {
    return 'info';
  } else if (successRate === 100) {
    return 'positive';
  } else if (successRate >= 50) {
    return 'yellow';
  } else {
    return 'negative';
  }
};

const ExecutionStatsBadge: React.FC<ExecutionStatsBadgeProps> = ({ stats }) => {
  const completedExecutions = stats
    ? Object.entries(stats.statusCount)
        .filter(([status]) => isExecutionCompleted(status as ExecutionStatus))
        .reduce((acc, [, count]) => acc + count, 0)
    : 0;

  const successfulExecutions = stats ? stats.statusCount[ExecutionStatus.SUCCEEDED] : 0;
  const successRate = completedExecutions > 0 ? (successfulExecutions / completedExecutions) * 100 : 0;
  const variant = getBadgeVariant(successRate, completedExecutions);

  return (
    <Badge variant={variant}>
      <Text>{`${successRate.toFixed(0)}% (${successfulExecutions}/${completedExecutions})`}</Text>
    </Badge>
  );
};

export default ExecutionStatsBadge;
