import { isProduction } from './node.ts';

export type Executable = {
  id: string;
  content: string;
};

export const ScriptRoot = '/conf/acm/settings/script';
export const ScriptRoots: Record<ScriptType, string> = {
  MANUAL: '/conf/acm/settings/script/manual',
  AUTOMATIC: '/conf/acm/settings/script/automatic',
  EXTENSION: '/conf/acm/settings/script/extension',
  MOCK: '/conf/acm/settings/script/mock',
};

export const ConsoleDefaultScriptPath = `${ScriptRoot}/template/core/console.groovy`;
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
  return id.startsWith(ScriptRoot);
}

export type Description = {
  execution: Execution;
  inputs: {
    [name: string]: Input<InputValue>;
  };
};

export type InputType = 'BOOL' | 'STRING' | 'TEXT' | 'SELECT' | 'MULTISELECT' | 'INTEGER' | 'DECIMAL' | 'DATETIME' | 'DATE' | 'TIME' | 'COLOR' | 'NUMBER_RANGE' | 'PATH' | 'FILE' | 'MULTIFILE' | 'MAP' | 'KEY_VALUE_LIST';
export type InputValue = string | string[] | number | number[] | boolean | null | undefined | RangeValue | KeyValue | MapValue;
export type InputValues = Record<string, InputValue>;

export const InputGroupDefault = 'general';

export type Input<T> = {
  name: string;
  type: InputType;
  value: T;
  label: string;
  description?: string;
  required: boolean;
  group: string;
  validator?: string;
};

export type Outputs = {
  [key: string]: Output
}

export type Output = {
  name: string;
  label: string;
  description?: string
  mimeType: string
  downloadName: string
}

export type MinMaxInput = Input<InputValue> & {
  min: number;
  max: number;
};

export type BoolInput = Input<boolean> & {
  display: 'SWITCHER' | 'CHECKBOX';
};

export type DateTimeInput = Input<string> & {
  min: string;
  max: string;
};

export type TextInput = Input<string> & {
  language?: string;
};

export type StringInput = Input<string> & {
  display: 'PLAIN' | 'PASSWORD';
};

export type NumberInput = Input<number> &
  MinMaxInput & {
    step: number;
    display: 'INPUT' | 'SLIDER';
  };

export type ColorInput = Input<string> & {
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

export type NumberRangeInput = Input<RangeValue> &
  MinMaxInput & {
    step: number;
  };

export type SelectInput = Input<InputValue> & {
  options: Record<string, InputValue>;
  display: 'AUTO' | 'DROPDOWN' | 'RADIO';
};

export type MultiSelectInput = Input<InputValue> & {
  options: Record<string, InputValue>;
  display: 'AUTO' | 'CHECKBOX' | 'DROPDOWN';
};

export type PathInput = Input<InputValue> & {
  rootPath: string;
  rootInclusive: boolean;
};

export type FileInput = Input<InputValue> & {
  mimeTypes: string[];
};

export type MultiFileInput = Input<InputValue> &
  MinMaxInput & {
    mimeTypes: string[];
  };

type KeyValueBaseInput = {
  keyLabel: string;
  valueLabel: string;
};

export type MapInput = Input<MapValue> & KeyValueBaseInput & {};

export type KeyValueListInput = Input<KeyValue> & KeyValueBaseInput & {};

export function isStringInput(input: Input<InputValue>): input is StringInput {
  return input.type === 'STRING';
}

export function isBoolInput(input: Input<InputValue>): input is BoolInput {
  return input.type === 'BOOL';
}

export function isDateTimeInput(input: Input<InputValue>): input is DateTimeInput {
  return input.type === 'DATETIME' || input.type === 'DATE' || input.type === 'TIME';
}

export function isTextInput(arg: Input<InputValue>): arg is TextInput {
  return arg.type === 'TEXT';
}

export function isSelectInput(input: Input<InputValue>): input is SelectInput {
  return input.type === 'SELECT';
}

export function isMultiSelectInput(input: Input<InputValue>): input is MultiSelectInput {
  return input.type === 'MULTISELECT';
}

export function isNumberInput(input: Input<InputValue>): input is NumberInput {
  return input.type === 'INTEGER' || input.type === 'DECIMAL';
}

export function isColorInput(input: Input<InputValue>): input is ColorInput {
  return input.type === 'COLOR';
}

export function isRangeInput(input: Input<InputValue>): input is NumberRangeInput {
  return input.type === 'NUMBER_RANGE';
}

export function isPathInput(input: Input<InputValue>): input is PathInput {
  return input.type === 'PATH';
}

export function isFileInput(input: Input<InputValue>): input is FileInput {
  return input.type === 'FILE';
}

export function isMultiFileInput(input: Input<InputValue>): input is MultiFileInput {
  return input.type === 'MULTIFILE';
}

export function isMapInput(input: Input<InputValue>): input is MapInput {
  return input.type === 'MAP';
}

export function isKeyValueListInput(input: Input<InputValue>): input is KeyValueListInput {
  return input.type === 'KEY_VALUE_LIST';
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
  inputs: InputValues;
  outputs: Outputs;
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
  AUTOMATIC = 'AUTOMATIC',
  MANUAL = 'MANUAL',
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
  scriptManagementEnabled: boolean;
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

export const instancePrefix = isProduction() ? '' : 'http://localhost:5502';

export enum InstanceOsgiServicePid {
  SCRIPT_SCHEDULER = 'dev.vml.es.acm.core.script.ScriptScheduler',

  CODE_EXECUTOR = 'dev.vml.es.acm.core.code.Executor',
  CODE_EXECUTION_QUEUE = 'dev.vml.es.acm.core.code.ExecutionQueue',
  SLING_QUEUE = 'org.apache.sling.event.jobs.QueueConfiguration~acmexecutionqueue',

  SPA_SETTINGS = 'dev.vml.es.acm.core.gui.SpaSettings',
  CODE_REPOSITORY = 'dev.vml.es.acm.core.code.CodeRepository',
  CODE_ASSISTANCER = 'dev.vml.es.acm.core.assist.Assistancer',
  MOCK_HTTP_FILTER = 'dev.vml.es.acm.core.mock.MockHttpFilter',
  INSTANCE_INFO = 'dev.vml.es.acm.core.osgi.InstanceInfo',

  NOTIFICATION_SLACK_FACTORY = 'dev.vml.es.acm.core.notification.slack.SlackFactory',
  NOTIFICATION_TEAMS_FACTORY = 'dev.vml.es.acm.core.notification.teams.TeamsFactory',
}

export function instanceOsgiServiceConfigUrl(pid: InstanceOsgiServicePid): string {
  return `${instancePrefix}/system/console/configMgr/${pid}`;
}

export type HealthStatus = {
  healthy: boolean;
  issues: HealthIssue[];
};

export type HealthIssue = {
  category: HealthIssueCategory;
  issue: string;
  details: string;
  severity: HealthIssueSeverity;
};

export enum HealthIssueCategory {
  INSTANCE = 'INSTANCE',
  REPOSITORY = 'REPOSITORY',
  OSGI = 'OSGI',
  INSTALLER = 'INSTALLER',
  CODE_EXECUTOR = 'CODE_EXECUTOR',
  OTHER = 'OTHER',
}

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
  LIMIT = 'limit',
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

export enum EventType {
  EXECUTOR_RESET = 'executor_reset',
  HISTORY_CLEAR = 'history_clear',
  SCRIPT_SCHEDULER_BOOT = 'script_scheduler_boot',
}
