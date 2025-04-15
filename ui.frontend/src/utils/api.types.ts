export type Executable = {
  id: string;
  content: string;
  arguments: ArgumentValues;
};

export const ExecutableIdConsole = 'console';

export function isExecutableConsole(id: string): boolean {
  return id === ExecutableIdConsole;
}
export function isExecutableScript(id: string): boolean {
  return id.startsWith('/conf/acm/settings/script');
}
export function isExecutableExplicit(id: string): boolean {
  return isExecutableConsole(id) || isExecutableScript(id);
}

export type Description = {
  execution: Execution;
  arguments: {
    [name: string]: Argument<ArgumentValue>;
  };
};

export type ArgumentType = 'BOOL' | 'STRING' | 'TEXT' | 'SELECT' | 'MULTISELECT' | 'INTEGER' | 'DECIMAL';
export type ArgumentValue = string | string[] | number | number[] | boolean | null | undefined;
export type ArgumentValues = Record<string, ArgumentValue>;

export const ArgumentGroupDefault = 'general';

export type Argument<T> = {
  name: string;
  type: ArgumentType;
  value: T;
  label: string;
  required: boolean;
  group: string;
  validator?: string;
};

export type BoolArgument = Argument<boolean> & {
  display: 'SWITCHER' | 'CHECKBOX';
};

export type TextArgument = Argument<string> & {
  language?: string;
};

export type NumberArgument = Argument<number> & {
  min: number;
  max: number;
};

export type SelectArgument = Argument<ArgumentValue> & {
  options: Record<string, ArgumentValue>;
  display: 'AUTO' | 'DROPDOWN' | 'RADIO';
};

export type MultiSelectArgument = Argument<ArgumentValue> & {
  options: Record<string, ArgumentValue>;
  display: 'AUTO' | 'CHECKBOX' | 'DROPDOWN';
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
  return arg.type === 'INTEGER' || arg.type === 'DECIMAL';
}

export function isMultiSelectArgument(arg: Argument<ArgumentValue>): arg is MultiSelectArgument {
  return arg.type === 'MULTISELECT';
}

export type Execution = {
  id: string;
  userId: string;
  executable: Executable;
  status: ExecutionStatus;
  startDate: string;
  endDate: string;
  duration: number;
  output: string;
  error: string | null;
};

export type ExecutionSummary = {
  id: string;
  userId: string;
  executableId: string;
  status: ExecutionStatus;
  startDate: string;
  endDate: string;
  duration: number;
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

export function isExecutionNegative(status: ExecutionStatus | null | undefined): boolean {
  return !!status && [ExecutionStatus.FAILED, ExecutionStatus.ABORTED].includes(status);
}

export function isExecutionPending(status: ExecutionStatus | null | undefined): boolean {
  return !!status && (status === ExecutionStatus.QUEUED || isExecutionActive(status));
}

export function isExecutionActive(status: ExecutionStatus | null | undefined): boolean {
  return !!status && [ExecutionStatus.ACTIVE, ExecutionStatus.PARSING, ExecutionStatus.CHECKING, ExecutionStatus.RUNNING].includes(status);
}

export function isExecutionCompleted(status: ExecutionStatus | null | undefined): boolean {
  return !!status && [ExecutionStatus.FAILED, ExecutionStatus.SUCCEEDED].includes(status);
}

export type QueueOutput = {
  executions: Execution[];
};

export type ExecutionOutput<E> = {
  list: E[];
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

export enum ScriptType {
  MANUAL = 'MANUAL',
  ENABLED = 'ENABLED',
  DISABLED = 'DISABLED',
  EXTENSION = 'EXTENSION',
}

export type Script = {
  id: string;
  type: ScriptType;
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
  spaSettings: SpaSettings;
  healthStatus: HealthStatus;
  instanceSettings: InstanceSettings;
  queuedExecutions: ExecutionSummary[];
};

export type SpaSettings = {
  appStateInterval: number;
  executionPollInterval: number;
};

export type InstanceSettings = {
  id: string;
  timezoneId: string;
  role: InstanceRole;
  type: InstanceType;
};

export enum InstanceRole {
  AUTHOR = 'AUTHOR',
  PUBLISH = 'PUBLISH',
}

export enum InstanceType {
  ON_PREM = 'ON_PREM',
  CLOUD_SDK = 'CLOUD_SDK',
  CLOUD_CONTAINER = 'CLOUD_CONTAINER',
}

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

export enum ExecutionFormat {
  SUMMARY = 'SUMMARY',
  FULL = 'FULL',
}

export enum ExecutionQueryParams {
  FORMAT = 'format',
  START_DATE = 'startDate',
  END_DATE = 'endDate',
  STATUS = 'status',
  EXECUTABLE_ID = 'executableId',
  DURATION = 'duration',
}
