export type Executable = {
    id: string;
    content: string;
};

export type Execution = {
    id: string;
    executable: Executable;
    status: ExecutionStatus;
    startDate: string;
    endDate: string;
    duration: number;
    output: string;
    error: string | null;
};

export enum ExecutionStatus {
    QUEUED = 'QUEUED',
    ACTIVE = 'ACTIVE',
    STOPPED = 'STOPPED',
    FAILED = 'FAILED',
    SKIPPED = 'SKIPPED',
    ABORTED = 'ABORTED',
    SUCCEEDED = 'SUCCEEDED',
}

export function isExecutionPending(status: ExecutionStatus | null) {
    return status === ExecutionStatus.QUEUED || status === ExecutionStatus.ACTIVE;
}

export function isExecutionCompleted(status: ExecutionStatus | null) {
    return status === ExecutionStatus.FAILED || status === ExecutionStatus.SUCCEEDED;
}

export type QueueOutput = {
    executions: Execution[];
};

export type ExecutionOutput = {
    list: Execution[];
};

export type AssistCodeOutput = {
    code: string;
    suggestions: {
        k: string; // kind
        l: string; // label
        it: string; // insert text
        i: string; // info
    }[];
};

export type SnippetOutput = {
    list: Snippet[];
};

export type Snippet = {
    name: string;
    id: string;
    group: string;
    content: string;
    documentation: string;
};

export type Script = {
    id: string;
    path: string;
    name: string;
    content: string;
};

export type ScriptStats = {
    path: string;
    statusCount: { [key in ExecutionStatus]: number };
    lastExecution: Execution | null;
};

export type ScriptOutput = {
    list: Script[];
    stats: ScriptStats[];
};

export type State = {
    healthStatus: HealthStatus;
    instanceSettings: InstanceSettings;
}

export type InstanceSettings = {
    id: string;
    timezoneId: string;
}

export type HealthStatus = {
    healthy: boolean;
    issues: HealthIssue[];
}

export type HealthIssue = {
    message: string;
    severity: HealthIssueSeverity;
}

export enum HealthIssueSeverity {
    CRITICAL = 'CRITICAL',
    WARNING = 'WARNING',
    INFO = 'INFO',
}