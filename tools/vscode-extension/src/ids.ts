/** Identifiers shared with package.json; `npm run check:manifest` fails when the two drift apart. */
export const NAMESPACE = 'acm';

/** The name users see for output channels and diagnostics. */
export const DISPLAY_NAME = 'ACM';

export const COMMANDS = {
  run: 'acm.run',
  runSelection: 'acm.runSelection',
  runWithoutHistory: 'acm.runWithoutHistory',
  runWithInputs: 'acm.runWithInputs',
  validate: 'acm.validate',
  describe: 'acm.describe',
  abort: 'acm.abort',
  selectInstance: 'acm.selectInstance',
  setCredentials: 'acm.setCredentials',
  checkConnection: 'acm.checkConnection',
  refreshExecutions: 'acm.refreshExecutions',
  refreshScripts: 'acm.refreshScripts',
  refreshProjectContent: 'acm.refreshProjectContent',
  filterExecutions: 'acm.filterExecutions',
  clearExecutionsFilter: 'acm.clearExecutionsFilter',
  filterExecutionsByScript: 'acm.filterExecutionsByScript',
  runScript: 'acm.runScript',
  compareScript: 'acm.compareScript',
  compareProjectScript: 'acm.compareProjectScript',
  runProjectScript: 'acm.runProjectScript',
  newProjectContent: 'acm.newProjectContent',
  newProjectScriptOfType: 'acm.newProjectScriptOfType',
  newProjectSnippet: 'acm.newProjectSnippet',
  renameProjectFile: 'acm.renameProjectFile',
  duplicateProjectFile: 'acm.duplicateProjectFile',
  deleteProjectFile: 'acm.deleteProjectFile',
  revealProjectFile: 'acm.revealProjectFile',
  newScript: 'acm.newScript',
  downloadOutputs: 'acm.downloadOutputs',
  copyMcpSetup: 'acm.copyMcpSetup',
  openUi: 'acm.openUi',
  openScriptInUi: 'acm.openScriptInUi',
  openExecutionInUi: 'acm.openExecutionInUi',
} as const;

export const VIEWS = {
  projectContent: 'acm.projectContent',
  scripts: 'acm.scripts',
  executions: 'acm.executions',
} as const;

/** Context keys that `when` clauses in package.json read. */
export const CONTEXT = {
  hasInstance: 'acm.hasInstance',
  canRunWithoutHistory: 'acm.canRunWithoutHistory',
  executionsFiltered: 'acm.executionsFiltered',
  hasProjectContent: 'acm.hasProjectContent',
  editingInputs: 'acm.editingInputs',
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
  scriptsRoots: 'scripts.roots',
  mockMode: 'mock.mode',
} as const;

export type SettingKey = (typeof SETTINGS)[keyof typeof SETTINGS];

export function settingId(key: SettingKey): string {
  return `${NAMESPACE}.${key}`;
}

/** Tree item context values that `viewItem` clauses in package.json match. */
export const ITEMS = {
  execution: 'execution',
  executionPending: 'execution.pending',
  projectRoot: 'projectRoot',
  projectScriptType: 'projectGroup.scriptType',
  projectSnippets: 'projectGroup.snippets',
  projectSnippet: 'projectFile.snippet',
  script: (type: string) => `script.${type.toLowerCase()}`,
  projectScript: (type: string) => `projectFile.script.${type.toLowerCase()}`,
} as const;

export const MCP_PROVIDER_ID = NAMESPACE;

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
