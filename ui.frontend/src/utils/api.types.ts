import { isProduction } from './node.ts';

export type Executable = {
  id: string;
  content: string;
  arguments: ArgumentValues;
};

export const ScriptRootPath = '/conf/acm/settings/script';
export const ConsoleDefaultScriptPath = `${ScriptRootPath}/template/core/console.groovy`;
export const ConsoleDefaultScriptContent = `
boolean canRun() {
  return conditions.always()
}
  
void doRun() {
  println "Hello World!"
}
`.trim();

export const ExecutableIdConsole = 'console';

export function isExecutableConsole(id: string): boolean {
  return id === ExecutableIdConsole;
}

export function isExecutableScript(id: string): boolean {
  return id.startsWith(ScriptRootPath);
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

export type ArgumentType = 'BOOL' | 'STRING' | 'TEXT' | 'SELECT' | 'MULTISELECT' | 'INTEGER' | 'DECIMAL' | 'DATETIME' | 'DATE' | 'TIME' | 'COLOR' | 'NUMBER_RANGE' | 'PATH' | 'FILE' | 'MULTIFILE' | 'MAP' | 'KEY_VALUE_LIST';
export type ArgumentValue = string | string[] | number | number[] | boolean | null | undefined | RangeValue | KeyValue | MapValue;
export type ArgumentValues = Record<string, ArgumentValue>;

export const ArgumentGroupDefault = 'general';

export type Argument<T> = {
  name: string;
  type: ArgumentType;
  value: T;
  label: string;
  description?: string;
  required: boolean;
  group: string;
  validator?: string;
};

export type MinMaxArgument = Argument<ArgumentValue> & {
  min: number;
  max: number;
};

export type BoolArgument = Argument<boolean> & {
  display: 'SWITCHER' | 'CHECKBOX';
};

export type DateTimeArgument = Argument<string> & {
  min: string;
  max: string;
};

export type TextArgument = Argument<string> & {
  language?: string;
};

export type StringArgument = Argument<string> & {
  display: 'PLAIN' | 'PASSWORD';
};

export type NumberArgument = Argument<number> &
  MinMaxArgument & {
    step: number;
    display: 'INPUT' | 'SLIDER';
  };

export type ColorArgument = Argument<string> & {
  format: 'HEX' | 'RGBA' | 'HSL' | 'HSB';
};

export type RangeValue = {
  start: number;
  end: number;
};

export type KeyValue = {
  key: string;
  value: string;
};

export type MapValue = Record<string, string>;

export type NumberRangeArgument = Argument<RangeValue> &
  MinMaxArgument & {
    step: number;
  };

export type SelectArgument = Argument<ArgumentValue> & {
  options: Record<string, ArgumentValue>;
  display: 'AUTO' | 'DROPDOWN' | 'RADIO';
};

export type MultiSelectArgument = Argument<ArgumentValue> & {
  options: Record<string, ArgumentValue>;
  display: 'AUTO' | 'CHECKBOX' | 'DROPDOWN';
};

export type PathArgument = Argument<ArgumentValue> & {
  rootPath: string;
  rootInclusive: boolean;
};

export type FileArgument = Argument<ArgumentValue> & {
  mimeTypes: string[];
};

export type MultiFileArgument = Argument<ArgumentValue> &
  MinMaxArgument & {
    mimeTypes: string[];
  };

type KeyValueBaseArgument = {
  keyLabel: string;
  valueLabel: string;
};

export type MapArgument = Argument<MapValue> & KeyValueBaseArgument & {};

export type KeyValueListArgument = Argument<KeyValue> & KeyValueBaseArgument & {};

export function isStringArgument(arg: Argument<ArgumentValue>): arg is StringArgument {
  return arg.type === 'STRING';
}

export function isBoolArgument(arg: Argument<ArgumentValue>): arg is BoolArgument {
  return arg.type === 'BOOL';
}

export function isDateTimeArgument(arg: Argument<ArgumentValue>): arg is DateTimeArgument {
  return arg.type === 'DATETIME' || arg.type === 'DATE' || arg.type === 'TIME';
}

export function isTextArgument(arg: Argument<ArgumentValue>): arg is TextArgument {
  return arg.type === 'TEXT';
}

export function isSelectArgument(arg: Argument<ArgumentValue>): arg is SelectArgument {
  return arg.type === 'SELECT';
}

export function isMultiSelectArgument(arg: Argument<ArgumentValue>): arg is MultiSelectArgument {
  return arg.type === 'MULTISELECT';
}

export function isNumberArgument(arg: Argument<ArgumentValue>): arg is NumberArgument {
  return arg.type === 'INTEGER' || arg.type === 'DECIMAL';
}

export function isColorArgument(arg: Argument<ArgumentValue>): arg is ColorArgument {
  return arg.type === 'COLOR';
}

export function isRangeArgument(arg: Argument<ArgumentValue>): arg is NumberRangeArgument {
  return arg.type === 'NUMBER_RANGE';
}

export function isPathArgument(arg: Argument<ArgumentValue>): arg is PathArgument {
  return arg.type === 'PATH';
}

export function isFileArgument(arg: Argument<ArgumentValue>): arg is FileArgument {
  return arg.type === 'FILE';
}

export function isMultiFileArgument(arg: Argument<ArgumentValue>): arg is MultiFileArgument {
  return arg.type === 'MULTIFILE';
}

export function isMapArgument(arg: Argument<ArgumentValue>): arg is MapArgument {
  return arg.type === 'MAP';
}

export function isKeyValueListArgument(arg: Argument<ArgumentValue>): arg is KeyValueListArgument {
  return arg.type === 'KEY_VALUE_LIST';
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

export const UserIdServicePrefix = 'acm-';

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
  suggestions: Suggestion[];
};

export enum SuggestionKind {
  VARIABLE = 'variable',
  RESOURCE = 'resource',
  SNIPPET = 'snippet',
  CLASS = 'class',
}

export type Suggestion = {
  k: SuggestionKind; // kind
  l: string; // label
  it: string; // insert text
  i: string; // info
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
  MOCK = 'MOCK',
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
  lastExecution: ExecutionSummary | null;
  averageDuration: number;
};

export type ScriptOutput = {
  list: Script[];
  stats: ScriptStats[];
};

export type State = {
  spaSettings: SpaSettings;
  healthStatus: HealthStatus;
  mockStatus: MockStatus;
  instanceSettings: InstanceSettings;
};

export type SpaSettings = {
  appStateInterval: number;
  executionPollInterval: number;
  scriptStatsLimit: number;
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

export const instancePrefix = isProduction() ? '' : 'http://localhost:4502';

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

export type MockStatus = {
  enabled: boolean;
};

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
  USER_ID = 'userId',
  DURATION = 'duration',
}

export enum NodeType {
  FOLDER = 'nt:folder',
  ORDERED_FOLDER = 'sling:OrderedFolder',
  SLING_FOLDER = 'sling:Folder',
  CQ_PROJECTS = 'cq/projects',
  REDIRECT = 'sling:redirect',
  ACL = 'rep:ACL',
  PAGE = 'cq:Page',
  FILE = 'nt:file',
}

export enum JCR_CONSTANTS {
  JCR_CONTENT = 'jcr:content',
}

export type FileOutput = {
  files: string[];
};
