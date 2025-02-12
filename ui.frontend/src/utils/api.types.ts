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
  error: string;
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
  scriptId: string;
  statusCount: { [key in ExecutionStatus]: number };
  lastExecution: Execution | null;
};

export type ScriptOutput = {
  list: Script[];
  stats: ScriptStats[];
};
