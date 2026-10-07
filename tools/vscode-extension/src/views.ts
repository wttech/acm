import * as vscode from 'vscode';
import {
  ACM_API,
  AcmHttpError,
  EXECUTION_STATUSES,
  executableIdOf,
  fetchConsoleOutput,
  fetchExecutionById,
  fetchExecutions,
  isExecutionFiltered,
  isPending,
  SCRIPT_ROOT,
  SCRIPT_TYPES,
  scriptLabel,
  summarizeExecution,
  type AcmClient,
  type Execution,
  type ExecutionFilter,
  type ExecutionStatus,
  type ScriptType,
} from '@acm/shared';
import { registerCommand } from './errors';
import {
  COMMANDS,
  CONTEXT,
  DOCUMENTS,
  ITEMS,
  NAMESPACE,
  SETTINGS,
  VIEWS,
  focusCommand,
  settingId,
} from './ids';
import { getActiveInstance, getClient, getInstances, getSettings, type AcmInstance } from './instances';

const SCHEME = DOCUMENTS.scheme;

export interface Script {
  id: string;
  path?: string;
  content?: string;
}

type Node =
  | { kind: 'message'; label: string; icon: string; command?: vscode.Command }
  | { kind: 'execution'; instance: AcmInstance; execution: Execution }
  | { kind: 'scriptType'; type: ScriptType; scripts: Script[] }
  | { kind: 'script'; instance: AcmInstance; script: Script; type: ScriptType };

export function executionUri(instance: AcmInstance, executionId: string): vscode.Uri {
  return vscode.Uri.from({ scheme: SCHEME, authority: DOCUMENTS.execution, path: `/${executionId}${DOCUMENTS.executionExtension}`, query: instance.name });
}

export function scriptUri(instance: AcmInstance, scriptId: string): vscode.Uri {
  return vscode.Uri.from({ scheme: SCHEME, authority: DOCUMENTS.script, path: scriptId, query: instance.name });
}

export function storedScript(uri: vscode.Uri): { id: string; instance: string } | undefined {
  return uri.scheme === SCHEME && uri.authority === DOCUMENTS.script ? { id: uri.path, instance: uri.query } : undefined;
}

/** Read-only documents for execution logs and stored scripts, fetched from the instance named in the query. */
class ContentProvider implements vscode.TextDocumentContentProvider {
  async provideTextDocumentContent(uri: vscode.Uri): Promise<string> {
    const instance = getInstances().find((candidate) => candidate.name === uri.query);
    const client = instance && (await getClient(instance));
    if (!client) {
      throw new Error(`ACM: No credentials for instance "${uri.query}".`);
    }
    if (uri.authority === DOCUMENTS.execution) {
      const id = uri.path.slice(1, -DOCUMENTS.executionExtension.length);
      const execution = await fetchExecutionById(client, id);
      if (!execution) {
        return `Execution '${id}' not found.`;
      }
      const output = isPending(execution.status) ? null : await fetchConsoleOutput(client, id);
      return summarizeExecution(execution, output, true);
    }
    const res = await client.request<{ list?: Script[] }>('GET', `${ACM_API.script}?id=${encodeURIComponent(uri.path)}`);
    return res.data?.list?.[0]?.content ?? '';
  }
}

abstract class AcmTreeProvider implements vscode.TreeDataProvider<Node> {
  private readonly changed = new vscode.EventEmitter<void>();
  readonly onDidChangeTreeData = this.changed.event;

  refresh(): void {
    this.changed.fire();
  }

  abstract getTreeItem(node: Node): vscode.TreeItem;

  async getChildren(node?: Node): Promise<Node[]> {
    const instance = getActiveInstance();
    if (!instance) {
      return [];
    }
    const client = await getClient(instance, false);
    if (!client) {
      return [
        {
          kind: 'message',
          label: `Set credentials for ${instance.name}`,
          icon: 'key',
          command: { title: 'Set Credentials', command: COMMANDS.setCredentials },
        },
      ];
    }
    try {
      return await this.fetch(instance, client, node);
    } catch (e) {
      const unauthorized = e instanceof AcmHttpError && e.httpStatus === 401;
      return [
        {
          kind: 'message',
          label: e instanceof Error ? e.message : String(e),
          icon: unauthorized ? 'key' : 'error',
          command: unauthorized ? { title: 'Set Credentials', command: COMMANDS.setCredentials } : undefined,
        },
      ];
    }
  }

  protected abstract fetch(instance: AcmInstance, client: AcmClient, node?: Node): Promise<Node[]>;

  protected messageItem(node: Extract<Node, { kind: 'message' }>): vscode.TreeItem {
    const item = new vscode.TreeItem(node.label);
    item.iconPath = new vscode.ThemeIcon(node.icon);
    item.tooltip = node.label;
    item.command = node.command;
    return item;
  }
}

class ExecutionsProvider extends AcmTreeProvider {
  filter: ExecutionFilter = { statuses: [] };

  describeFilter(): string | undefined {
    const { executableId, statuses } = this.filter;
    return [executableId && executableLabel(executableId), statuses.join(', ')].filter(Boolean).join(' · ') || undefined;
  }

  protected async fetch(instance: AcmInstance, client: AcmClient): Promise<Node[]> {
    const executions = await fetchExecutions(client, getSettings().executionsLimit, this.filter);
    if (executions.length === 0) {
      const label = isExecutionFiltered(this.filter) ? 'No executions match the filter' : 'No executions yet';
      return [{ kind: 'message', label, icon: 'info' }];
    }
    return executions.map((execution) => ({ kind: 'execution', instance, execution }));
  }

  getTreeItem(node: Node): vscode.TreeItem {
    if (node.kind !== 'execution') {
      return this.messageItem(node as Extract<Node, { kind: 'message' }>);
    }
    const { execution, instance } = node;
    const item = new vscode.TreeItem(executableLabel(executableIdOf(execution)));
    const started = execution.startDate ? new Date(execution.startDate) : undefined;
    item.description = [
      started && !isNaN(started.getTime()) ? started.toLocaleString() : execution.startDate,
      execution.userId,
    ]
      .filter(Boolean)
      .join(' · ');
    item.tooltip = [execution.id, [execution.status, item.description].join(' · '), execution.duration !== undefined && `${execution.duration} ms`]
      .filter(Boolean)
      .join('\n');
    item.iconPath = statusIcon(execution.status);
    item.contextValue = isPending(execution.status) ? ITEMS.executionPending : ITEMS.execution;
    item.command = { title: 'Open', command: 'vscode.open', arguments: [executionUri(instance, execution.id)] };
    return item;
  }
}

class ScriptsProvider extends AcmTreeProvider {
  protected async fetch(instance: AcmInstance, client: AcmClient, node?: Node): Promise<Node[]> {
    if (node?.kind === 'scriptType') {
      return node.scripts
        .map((script) => ({ kind: 'script' as const, instance, script, type: node.type }))
        .sort((a, b) => scriptLabel(a.script.id).localeCompare(scriptLabel(b.script.id)));
    }
    if (node) {
      return [];
    }
    const results = await Promise.allSettled(
      SCRIPT_TYPES.map(async (type) => ({
        type,
        scripts: (await client.request<{ list?: Script[] }>('GET', `${ACM_API.script}?type=${type}`)).data?.list ?? [],
      })),
    );
    const failed = results.find((result): result is PromiseRejectedResult => result.status === 'rejected');
    if (failed && results.every((result) => result.status === 'rejected')) {
      throw failed.reason;
    }
    const groups = results.flatMap((result) =>
      result.status === 'fulfilled' && result.value.scripts.length > 0
        ? [{ kind: 'scriptType' as const, type: result.value.type, scripts: result.value.scripts }]
        : [],
    );
    return groups.length > 0 ? groups : [{ kind: 'message', label: 'No scripts', icon: 'info' }];
  }

  getTreeItem(node: Node): vscode.TreeItem {
    switch (node.kind) {
      case 'scriptType': {
        const item = new vscode.TreeItem(
          node.type.charAt(0) + node.type.slice(1).toLowerCase(),
          vscode.TreeItemCollapsibleState.Expanded,
        );
        item.id = `scripts.${node.type}`;
        item.description = String(node.scripts.length);
        item.iconPath = vscode.ThemeIcon.Folder;
        return item;
      }
      case 'script': {
        const uri = scriptUri(node.instance, node.script.id);
        const item = new vscode.TreeItem(scriptLabel(node.script.id));
        item.tooltip = node.script.id;
        item.resourceUri = uri;
        item.contextValue = ITEMS.script(node.type);
        item.command = { title: 'Open', command: 'vscode.open', arguments: [uri] };
        return item;
      }
      default:
        return this.messageItem(node as Extract<Node, { kind: 'message' }>);
    }
  }
}

function executableLabel(id: string | undefined): string {
  return id?.startsWith(SCRIPT_ROOT) ? id.slice(SCRIPT_ROOT.length) : (id ?? '?');
}

const GREEN = 'testing.iconPassed';
const RED = 'testing.iconFailed';
const YELLOW = 'problemsWarningIcon.foreground';
const BLUE = 'problemsInfoIcon.foreground';

/** Icons and colors of the status badges in the ACM UI, so a status reads the same in both. */
const STATUS_ICONS: Record<ExecutionStatus, [icon: string, color?: string]> = {
  SUCCEEDED: ['pass', GREEN],
  FAILED: ['error', RED],
  ABORTED: ['circle-slash', RED],
  LOCKED: ['lock', YELLOW],
  QUEUED: ['clock', YELLOW],
  ACTIVE: ['sync~spin', BLUE],
  PARSING: ['sync~spin', BLUE],
  CHECKING: ['sync~spin', BLUE],
  RUNNING: ['sync~spin', BLUE],
  STOPPING: ['sync~spin', BLUE],
  STOPPED: ['debug-pause'],
  SKIPPED: ['debug-pause'],
};

function statusIcon(status: string): vscode.ThemeIcon {
  const [icon, color] = STATUS_ICONS[status.toUpperCase() as ExecutionStatus] ?? ['circle-outline'];
  return new vscode.ThemeIcon(icon, color ? new vscode.ThemeColor(color) : undefined);
}

export interface Views {
  refreshExecutions(): void;
}

export function registerViews(context: vscode.ExtensionContext): Views {
  const executions = new ExecutionsProvider();
  const scripts = new ScriptsProvider();
  const executionsView = vscode.window.createTreeView(VIEWS.executions, { treeDataProvider: executions });
  const scriptsView = vscode.window.createTreeView(VIEWS.scripts, { treeDataProvider: scripts });
  const describeScripts = () => {
    scriptsView.description = getActiveInstance()?.name;
  };
  describeScripts();
  const applyFilter = (filter: ExecutionFilter) => {
    executions.filter = filter;
    executionsView.description = executions.describeFilter();
    vscode.commands.executeCommand('setContext', CONTEXT.executionsFiltered, isExecutionFiltered(executions.filter));
    executions.refresh();
  };
  context.subscriptions.push(
    vscode.workspace.registerTextDocumentContentProvider(SCHEME, new ContentProvider()),
    executionsView,
    scriptsView,
    vscode.commands.registerCommand(COMMANDS.refreshExecutions, () => executions.refresh()),
    vscode.commands.registerCommand(COMMANDS.refreshScripts, () => scripts.refresh()),
    registerCommand(COMMANDS.filterExecutions, async () => {
      const filter = await pickExecutionFilter(executions.filter);
      if (filter) {
        applyFilter(filter);
      }
    }),
    registerCommand(COMMANDS.filterExecutionsByScript, async (node?: Node) => {
      if (node?.kind === 'script') {
        applyFilter({ ...executions.filter, executableId: node.script.id });
        await vscode.commands.executeCommand(focusCommand(VIEWS.executions));
      }
    }),
    registerCommand(COMMANDS.clearExecutionsFilter, () => applyFilter({ statuses: [] })),
    vscode.workspace.onDidChangeConfiguration((event) => {
      if (event.affectsConfiguration(settingId(SETTINGS.activeInstance)) && isExecutionFiltered(executions.filter)) {
        applyFilter({ statuses: [] });
      }
      if (event.affectsConfiguration(NAMESPACE)) {
        describeScripts();
        executions.refresh();
        scripts.refresh();
      }
    }),
    context.secrets.onDidChange(() => {
      executions.refresh();
      scripts.refresh();
    }),
  );
  return { refreshExecutions: () => executions.refresh() };
}

/** Asks what to filter by; `undefined` when cancelled. */
async function pickExecutionFilter(current: ExecutionFilter): Promise<ExecutionFilter | undefined> {
  const field = await vscode.window.showQuickPick(
    [
      { label: '$(file-code) Script', description: current.executableId && executableLabel(current.executableId), field: 'script' },
      { label: '$(pass) Status', description: current.statuses.join(', ') || undefined, field: 'status' },
    ],
    { title: 'Filter Executions', placeHolder: 'Filter by' },
  );
  if (!field) {
    return undefined;
  }
  if (field.field === 'status') {
    const items = EXECUTION_STATUSES.map((status) => ({ label: status, picked: current.statuses.includes(status) }));
    const picked = await vscode.window.showQuickPick(items, {
      title: 'Filter Executions by Status',
      placeHolder: 'Select statuses; none shows all',
      canPickMany: true,
    });
    return picked && { ...current, statuses: picked.map((item) => item.label) };
  }
  const instance = getActiveInstance();
  const client = instance && (await getClient(instance, false));
  if (!client) {
    return undefined;
  }
  const executions = await fetchExecutions(client, getSettings().executionsLimit);
  const ids = new Set(executions.map(executableIdOf).filter((id): id is string => !!id));
  const picked = await vscode.window.showQuickPick(
    [
      { label: 'Any script', id: undefined as string | undefined },
      ...[...ids].sort().map((id) => ({ label: executableLabel(id), id })),
    ],
    { title: 'Filter Executions by Script', placeHolder: 'Scripts from recent executions' },
  );
  return picked && { ...current, executableId: picked.id };
}
