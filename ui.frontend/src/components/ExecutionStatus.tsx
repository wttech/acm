import React from 'react';
import { Badge, Text } from '@adobe/react-spectrum';
import { ExecutionStatus as ExecutionStatusType } from '../utils/api.types';
import Alert from '@spectrum-icons/workflow/Alert';
import Clock from '@spectrum-icons/workflow/Clock';
import Pause from '@spectrum-icons/workflow/Pause';
import Cancel from "@spectrum-icons/workflow/Cancel";
import Checkmark from "@spectrum-icons/workflow/Checkmark";

interface ExecutionStatusProps {
    value: ExecutionStatusType;
}

const getBadgeVariant = (status: ExecutionStatusType): 'positive' | 'negative' | 'neutral' | 'info' | 'yellow' => {
    switch (status) {
        case ExecutionStatusType.SUCCEEDED:
            return 'positive';
        case ExecutionStatusType.FAILED:
            return 'negative';
        case ExecutionStatusType.ACTIVE:
            return 'info';
        case ExecutionStatusType.QUEUED:
            return 'yellow';
        case ExecutionStatusType.STOPPED:
        case ExecutionStatusType.SKIPPED:
            return 'neutral';
        case ExecutionStatusType.ABORTED:
            return 'negative';
        default:
            return 'neutral';
    }
};

const getIcon = (status: ExecutionStatusType) => {
    switch (status) {
        case ExecutionStatusType.SUCCEEDED:
            return <Checkmark/>;
        case ExecutionStatusType.FAILED:
            return <Alert/>;
        case ExecutionStatusType.ACTIVE:
            return <Clock/>;
        case ExecutionStatusType.QUEUED:
            return <Clock/>;
        case ExecutionStatusType.STOPPED:
            return <Pause/>;
        case ExecutionStatusType.SKIPPED:
            return <Pause/>;
        case ExecutionStatusType.ABORTED:
            return <Cancel/>;
        default:
            return null;
    }
};

const ExecutionStatus: React.FC<ExecutionStatusProps> = ({ value }) => {
    const variant = getBadgeVariant(value);
    const icon = getIcon(value);

    return (
        <Badge variant={variant}>
            <Text>{value.toLowerCase()}</Text>
            {icon}
        </Badge>
    );
};

export default ExecutionStatus;
