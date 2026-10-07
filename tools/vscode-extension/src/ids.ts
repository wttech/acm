/** Identifiers shared with package.json; `npm run check:manifest` fails when the two drift apart. */
export const NAMESPACE = 'acm';

/** The name users see for output channels and diagnostics. */
export const DISPLAY_NAME = 'ACM';

export const COMMANDS = {
  run: 'acm.run',
  runSelection: 'acm.runSelection',
  runWithoutHistory: 'acm.runWithoutHistory',
  validate: 'acm.validate',
  describe: 'acm.describe',
  abort: 'acm.abort',
  selectInstance: 'acm.selectInstance',
  setCredentials: 'acm.setCredentials',
  checkConnection: 'acm.checkConnection',
  refreshExecutions: 'acm.refreshExecutions',
  refreshScripts: 'acm.refreshScripts',
  filterExecutions: 'acm.filterExecutions',
  clearExecutionsFilter: 'acm.clearExecutionsFilter',
  filterExecutionsByScript: 'acm.filterExecutionsByScript',
  runScript: 'acm.runScript',
  compareScript: 'acm.compareScript',
  compareProjectScript: 'acm.compareProjectScript',
  runProjectScript: 'acm.runProjectScript',
  newProjectScript: 'acm.newProjectScript',
  renameProjectScript: 'acm.renameProjectScript',
  duplicateProjectScript: 'acm.duplicateProjectScript',
  deleteProjectScript: 'acm.deleteProjectScript',
  revealProjectScript: 'acm.revealProjectScript',
  selectScriptsRoot: 'acm.selectScriptsRoot',
  newScript: 'acm.newScript',
  downloadOutputs: 'acm.downloadOutputs',
  copyMcpSetup: 'acm.copyMcpSetup',
} as const;

export const VIEWS = {
  projectScripts: 'acm.projectScripts',
  scripts: 'acm.scripts',
  executions: 'acm.executions',
} as const;

/** Context keys that `when` clauses in package.json read. */
export const CONTEXT = {
  hasInstance: 'acm.hasInstance',
  canRunWithoutHistory: 'acm.canRunWithoutHistory',
  executionsFiltered: 'acm.executionsFiltered',
  hasScriptsRoot: 'acm.hasScriptsRoot',
} as const;

/** Setting keys relative to the `acm` section. */
export const SETTINGS = {
  instances: 'instances',
  activeInstance: 'activeInstance',
  validateOnSave: 'validateOnSave',
  mcpEnabled: 'mcp.enabled',
  httpTimeout: 'http.timeout',
  runTimeout: 'run.timeout',
  runPollInterval: 'run.pollInterval',
  healthInterval: 'health.interval',
  executionsLimit: 'executions.limit',
  scriptsRoot: 'scripts.root',
} as const;

export type SettingKey = (typeof SETTINGS)[keyof typeof SETTINGS];

export function settingId(key: SettingKey): string {
  return `${NAMESPACE}.${key}`;
}

/** Tree item context values that `viewItem` clauses in package.json match. */
export const ITEMS = {
  execution: 'execution',
  executionPending: 'execution.pending',
  projectType: 'projectType',
  script: (type: string) => `script.${type.toLowerCase()}`,
  projectScript: (type: string) => `projectScript.${type.toLowerCase()}`,
} as const;

export const MCP_PROVIDER_ID = NAMESPACE;

/** Keys of the per-workspace state the extension remembers. */
export const STATE_KEYS = {
  scriptsRoot: 'acm.scriptsRoot',
} as const;

/** Read-only documents of the `acm:` scheme: `acm://<authority>/...?<instance name>`. */
export const DOCUMENTS = {
  scheme: NAMESPACE,
  execution: 'execution',
  script: 'script',
  executionExtension: '.acmlog',
} as const;

export function focusCommand(view: (typeof VIEWS)[keyof typeof VIEWS]): string {
  return `${view}.focus`;
}
