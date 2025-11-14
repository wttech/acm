import { InstanceRole, InstanceType } from './aem.ts';
import { Execution, ExecutionStatus } from './execution.ts';
import { HealthStatus } from './health.ts';
import { Input, InputValue } from './input.ts';
import { Script, ScriptSchedule, ScriptStats } from './script.ts';
import { Snippet } from './snippet.ts';
import { Suggestion } from './suggestion.ts';

export type Description = {
  execution: Execution;
  inputs: {
    [name: string]: Input<InputValue>;
  };
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

export type SnippetOutput = {
  list: Snippet[];
};

export type State = {
  spaSettings: SpaSettings;
  healthStatus: HealthStatus;
  mockStatus: MockStatus;
  instanceSettings: InstanceSettings;
  permissions: Permissions;
};

export const StateDefault: State = {
  spaSettings: {
    appStateInterval: 3000,
    executionPollInterval: 1400,
    scriptStatsLimit: 20,
  },
  healthStatus: {
    healthy: true,
    issues: [],
  },
  mockStatus: {
    enabled: false,
  },
  instanceSettings: {
    id: 'default',
    timezoneId: 'UTC',
    role: InstanceRole.AUTHOR,
    type: InstanceType.CLOUD_CONTAINER,
  },
  permissions: {
    features: {
      console: true,
      'console.execute': true,
      dashboard: true,
      history: true,
      snippets: true,
      scripts: true,
      'scripts.manage': true,
      'scripts.execute': true,
      maintenance: true,
      'maintenance.manage': true,
    },
  },
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

export type MockStatus = {
  enabled: boolean;
};

export type Permissions = {
  features: Record<FeatureId, boolean>;
};

export type FeatureId = 'console' | 'console.execute' | 'dashboard' | 'history' | 'snippets' | 'scripts' | 'scripts.execute' | 'scripts.manage' | 'maintenance' | 'maintenance.manage';

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

export type FileOutput = {
  files: string[];
};

export enum EventType {
  EXECUTOR_RESET = 'executor_reset',
  HISTORY_CLEAR = 'history_clear',
  SCRIPT_SCHEDULER_BOOT = 'script_scheduler_boot',
}
export type ScriptOutput = {
  list: Script[];
  stats: ScriptStats[];
  schedules: ScriptSchedule[];
};
