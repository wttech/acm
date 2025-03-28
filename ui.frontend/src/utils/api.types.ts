export type Executable = {
  id: string;
  content: string;
};

export type Description = {
  execution: Execution
  arguments: {
    [name: string]: Argument<ArgumentValue>;
  };
};

export type ArgumentType = 'BOOL' | 'STRING' | 'TEXT' | 'SELECT' | 'INTEGER' | 'DOUBLE';
export type ArgumentValue = string | number | boolean | null | undefined;
export type ArgumentValues = Record<string, ArgumentValue>

export const ArgumentGroupDefault = "general";

export type Argument<T> = {
  name: string;
  type: ArgumentType;
  value: T;
  label: string;
  required: boolean;
  group: string;
};

export type BoolArgument = Argument<boolean> & {
  display: 'SWITCHER' | 'CHECKBOX';
}

export type TextArgument = Argument<string> & {
  language?: string;
};

export type SelectArgument = Argument<ArgumentValue> & {
  options: Record<string, ArgumentValue>;
};

export type NumberArgument = Argument<number> & {
  min: number;
  max: number;
};

export function isStringArgument(arg: Argument<ArgumentValue>): arg is Argument<string> {
  return arg.type === 'STRING';
}

export function isBoolArgument(arg: Argument<ArgumentValue>): arg is BoolArgument {
  return arg.type === 'BOOL';
}

export function isTextArgument(arg: Argument<ArgumentValue>): arg is TextArgument {
  return arg.type === 'TEXT';
}

export function isSelectArgument(arg: Argument<ArgumentValue>): arg is SelectArgument {
  return arg.type === 'SELECT';
}

export function isNumberArgument(arg: Argument<ArgumentValue>): arg is NumberArgument {
  return arg.type === 'INTEGER' || arg.type === 'DOUBLE';
}

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
  PARSING = 'PARSING',
  CHECKING = 'CHECKING',
  RUNNING = 'RUNNING',
  STOPPED = 'STOPPED',
  FAILED = 'FAILED',
  SKIPPED = 'SKIPPED',
  ABORTED = 'ABORTED',
  SUCCEEDED = 'SUCCEEDED',
}

export function isExecutionNegative(status: ExecutionStatus | null | undefined) {
  return status && [ExecutionStatus.FAILED, ExecutionStatus.ABORTED].includes(status);
}

export function isExecutionPending(status: ExecutionStatus | null | undefined) {
  return status === ExecutionStatus.QUEUED || isExecutionActive(status);
}

export function isExecutionActive(status: ExecutionStatus | null | undefined) {
  return status && [ExecutionStatus.ACTIVE, ExecutionStatus.PARSING, ExecutionStatus.CHECKING, ExecutionStatus.RUNNING].includes(status);
}

export function isExecutionCompleted(status: ExecutionStatus | null | undefined) {
  return status && [ExecutionStatus.FAILED, ExecutionStatus.SUCCEEDED].includes(status);
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
  queuedExecutions: Execution[];
};

export type InstanceSettings = {
  id: string;
  timezoneId: string;
};

export type HealthStatus = {
  healthy: boolean;
  issues: HealthIssue[];
};

export type HealthIssue = {
  message: string;
  severity: HealthIssueSeverity;
};

export enum HealthIssueSeverity {
  CRITICAL = 'CRITICAL',
  WARNING = 'WARNING',
  INFO = 'INFO',
}
