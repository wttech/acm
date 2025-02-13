import { Badge, Text } from '@adobe/react-spectrum';
import React from 'react';
import { ExecutionStatus, Script, ScriptStats } from '../utils/api.types';
import { useFormatter } from '../utils/hooks.ts';

interface ExecutionStatsBadgeProps {
    script: Script;
    stats: ScriptStats;
}

const getBadgeVariant = (successRate: number): 'positive' | 'negative' | 'neutral' | 'info' | 'yellow' => {
    if (successRate === 100) {
        return 'positive';
    } else if (successRate > 80) {
        return 'yellow';
    } else {
        return 'negative';
    }
};

const ExecutionStatsBadge: React.FC<ExecutionStatsBadgeProps> = ({ script, stats }) => {
    const formatter = useFormatter();
    const successfulExecutions = stats ? stats.statusCount[ExecutionStatus.SUCCEEDED] : 0;
    const totalExecutions = stats ? Object.values(stats.statusCount).reduce((a, b) => a + b, 0) : 0;
    const successRate = totalExecutions > 0 ? (successfulExecutions / totalExecutions) * 100 : 0;
    const variant = getBadgeVariant(successRate);

    return (
        <Badge variant={variant}>
            <Text>{`${successRate.toFixed(0)}% (${successfulExecutions}/${totalExecutions})`}</Text>
        </Badge>
    );
};

export default ExecutionStatsBadge;