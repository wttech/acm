import * as path from 'node:path';
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
  isFailed,
  isPending,
  summarizeExecution,
  type AcmClient,
  type Execution,
  type ExecutionFilter,
} from '@acm/shared';
import { registerCommand } from './errors';
import { getActiveInstance, getClient, getInstances, getSettings, type AcmInstance } from './instances';

export const SCHEME = 'acm';
const SCRIPT_ROOT = '/conf/acm/settings/script/';
const SCRIPT_TYPES = ['MANUAL', 'AUTOMATIC', 'EXTENSION', 'MOCK'];

interface Script {
  id: string;
  path?: string;
  content?: string;
}

type Node =
  | { kind: 'message'; label: string; icon: string; command?: vscode.Command }
  | { kind: 'execution'; instance: AcmInstance; execution: Execution }
  | { kind: 'scriptType'; instance: AcmInstance; type: string }
  | { kind: 'script'; instance: AcmInstance; script: Script; type: string };

export function executionUri(instance: AcmInstance, executionId: string): vscode.Uri {
  return vscode.Uri.from({ scheme: SCHEME, authority: 'execution', path: `/${executionId}.acmlog`, query: instance.name });
}

export function scriptUri(instance: AcmInstance, scriptId: string): vscode.Uri {
  return vscode.Uri.from({ scheme: SCHEME, authority: 'script', path: scriptId, query: instance.name });
}

export function storedScript(uri: vscode.Uri): { id: string; instance: string } | undefined {
  return uri.scheme === SCHEME && uri.authority === 'script' ? { id: uri.path, instance: uri.query } : undefined;
}

/** Read-only documents for execution logs and stored scripts, fetched from the instance named in the query. */
class ContentProvider implements vscode.TextDocumentContentProvider {
  async provideTextDocumentContent(uri: vscode.Uri): Promise<string> {
    const instance = getInstances().find((candidate) => candidate.name === uri.query);
    const client = instance && (await getClient(instance));
    if (!client) {
      throw new Error(`ACM: No credentials for instance "${uri.query}".`);
    }
    if (uri.authority === 'execution') {
      const id = uri.path.slice(1).replace(/\.acmlog$/, '');
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
          command: { title: 'Set Credentials', command: 'acm.setCredentials' },
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
          command: unauthorized ? { title: 'Set Credentials', command: 'acm.setCredentials' } : undefined,
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
      execution.status,
      started && !isNaN(started.getTime()) ? started.toLocaleString() : execution.startDate,
      execution.userId,
    ]
      .filter(Boolean)
      .join(' · ');
    item.tooltip = `${execution.id}\n${item.description}${execution.duration !== undefined ? `\n${execution.duration} ms` : ''}`;
    item.iconPath = statusIcon(execution.status);
    item.contextValue = isPending(execution.status) ? 'execution.pending' : 'execution';
    item.command = { title: 'Open', command: 'vscode.open', arguments: [executionUri(instance, execution.id)] };
    return item;
  }
}

class ScriptsProvider extends AcmTreeProvider {
  protected async fetch(instance: AcmInstance, client: AcmClient, node?: Node): Promise<Node[]> {
    if (!node) {
      return SCRIPT_TYPES.map((type) => ({ kind: 'scriptType', instance, type }));
    }
    if (node.kind !== 'scriptType') {
      return [];
    }
    const res = await client.request<{ list?: Script[] }>('GET', `${ACM_API.script}?type=${node.type}`);
    const scripts = res.data?.list ?? [];
    if (scripts.length === 0) {
      return [{ kind: 'message', label: 'No scripts', icon: 'info' }];
    }
    return scripts.map((script) => ({ kind: 'script', instance, script, type: node.type }));
  }

  getTreeItem(node: Node): vscode.TreeItem {
    switch (node.kind) {
      case 'scriptType': {
        const item = new vscode.TreeItem(
          node.type.charAt(0) + node.type.slice(1).toLowerCase(),
          vscode.TreeItemCollapsibleState.Collapsed,
        );
        item.iconPath = vscode.ThemeIcon.Folder;
        return item;
      }
      case 'script': {
        const uri = scriptUri(node.instance, node.script.id);
        const item = new vscode.TreeItem(path.posix.basename(node.script.id));
        item.description = path.posix.dirname(executableLabel(node.script.id)).replace(/^\.$/, '');
        item.tooltip = node.script.id;
        item.resourceUri = uri;
        item.contextValue = `script.${node.type.toLowerCase()}`;
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

function statusIcon(status: string): vscode.ThemeIcon {
  if (isPending(status)) {
    return new vscode.ThemeIcon('sync~spin');
  }
  switch (status.toUpperCase()) {
    case 'SUCCEEDED':
      return new vscode.ThemeIcon('pass', new vscode.ThemeColor('testing.iconPassed'));
    case 'SKIPPED':
      return new vscode.ThemeIcon('debug-step-over');
    default:
      return isFailed(status)
        ? new vscode.ThemeIcon('error', new vscode.ThemeColor('testing.iconFailed'))
        : new vscode.ThemeIcon('circle-outline');
  }
}

export interface Views {
  refreshExecutions(): void;
}

export function registerViews(context: vscode.ExtensionContext): Views {
  const executions = new ExecutionsProvider();
  const scripts = new ScriptsProvider();
  const executionsView = vscode.window.createTreeView('acm.executions', { treeDataProvider: executions });
  const applyFilter = (filter: ExecutionFilter) => {
    executions.filter = filter;
    executionsView.description = executions.describeFilter();
    vscode.commands.executeCommand('setContext', 'acm.executionsFiltered', isExecutionFiltered(executions.filter));
    executions.refresh();
  };
  context.subscriptions.push(
    vscode.workspace.registerTextDocumentContentProvider(SCHEME, new ContentProvider()),
    executionsView,
    vscode.window.registerTreeDataProvider('acm.scripts', scripts),
    vscode.commands.registerCommand('acm.refreshExecutions', () => executions.refresh()),
    vscode.commands.registerCommand('acm.refreshScripts', () => scripts.refresh()),
    registerCommand('acm.filterExecutions', async () => {
      const filter = await pickExecutionFilter(executions.filter);
      if (filter) {
        applyFilter(filter);
      }
    }),
    registerCommand('acm.filterExecutionsByScript', async (node?: Node) => {
      if (node?.kind === 'script') {
        applyFilter({ ...executions.filter, executableId: node.script.id });
        await vscode.commands.executeCommand('acm.executions.focus');
      }
    }),
    registerCommand('acm.clearExecutionsFilter', () => applyFilter({ statuses: [] })),
    registerCommand('acm.compareScript', (node?: Node) => compareScript(node)),
    vscode.workspace.onDidChangeConfiguration((event) => {
      if (event.affectsConfiguration('acm.activeInstance') && isExecutionFiltered(executions.filter)) {
        applyFilter({ statuses: [] });
      }
      if (event.affectsConfiguration('acm')) {
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

async function compareScript(node?: Node): Promise<void> {
  if (node?.kind !== 'script') {
    return;
  }
  const name = path.posix.basename(node.script.id);
  const files = await vscode.workspace.findFiles(`**/${name}`, '**/{node_modules,target}/**', 50);
  let local: vscode.Uri | undefined = files[0];
  if (files.length > 1) {
    local = (
      await vscode.window.showQuickPick(
        files.map((uri) => ({ label: vscode.workspace.asRelativePath(uri), uri })),
        { placeHolder: `Local file to compare with ${name}` },
      )
    )?.uri;
  } else if (files.length === 0) {
    local = (await vscode.window.showOpenDialog({ canSelectMany: false, filters: { Groovy: ['groovy'] } }))?.[0];
  }
  if (!local) {
    return;
  }
  await vscode.commands.executeCommand(
    'vscode.diff',
    scriptUri(node.instance, node.script.id),
    local,
    `${name} (${node.instance.name}) ↔ ${vscode.workspace.asRelativePath(local)}`,
  );
}
