import { Executable } from './executable';
import { ExecutionStatus } from './execution';
import { ExecutionSummary } from './main';

export enum ScriptType {
  AUTOMATIC = 'AUTOMATIC',
  MANUAL = 'MANUAL',
  EXTENSION = 'EXTENSION',
  MOCK = 'MOCK',
}

export type Script = Executable & {
  type: ScriptType;
  path: string;
  name: string;
};

export type ScriptStats = {
  path: string;
  statusCount: {
    [key in ExecutionStatus]: number;
  };
  lastExecution: ExecutionSummary | null;
  averageDuration: number;
};

export type ScriptSchedule = {
  path: string;
  nextExecution: string;
};

export const ScriptRoot = '/conf/acm/settings/script';

export const ScriptRoots: Record<ScriptType, string> = {
  MANUAL: '/conf/acm/settings/script/manual',
  AUTOMATIC: '/conf/acm/settings/script/automatic',
  EXTENSION: '/conf/acm/settings/script/extension',
  MOCK: '/conf/acm/settings/script/mock',
};
