import * as path from 'node:path';
import * as vscode from 'vscode';
import {
  ACM_API,
  AcmHttpError,
  SCRIPT_ROOT,
  SCRIPT_TEMPLATES,
  SCRIPT_TYPES,
  scriptIdOf,
  scriptLabel,
  scriptRootsOf,
  scriptTypeOf,
  validateScriptName,
  type ScriptType,
} from '@acm/shared';
import { registerCommand } from './errors';
import { getTarget, type AcmInstance } from './instances';
import { scriptUri } from './views';

const ROOT_STATE_KEY = 'acm.scriptsRoot';
const ROOT_GLOB = '**/jcr_root/conf/acm/settings/script/**';
const IGNORED_GLOB = '**/{node_modules,target,.git}/**';
// Types that always show, so a new script can be added to an empty one.
const ALWAYS_SHOWN: ScriptType[] = ['MANUAL', 'AUTOMATIC'];

interface LocalScript {
  uri: vscode.Uri;
  id: string;
  type: ScriptType;
}

type Node =
  | { kind: 'message'; label: string; icon: string; command?: vscode.Command }
  | { kind: 'type'; type: ScriptType; scripts: LocalScript[] }
  | { kind: 'script'; script: LocalScript };

interface Roots {
  root?: vscode.Uri;
  candidates: vscode.Uri[];
}

let state: vscode.Memento;
let provider: ProjectScriptsProvider;

function workspaceFolder(): vscode.WorkspaceFolder | undefined {
  return vscode.workspace.workspaceFolders?.[0];
}

function fileExists(uri: vscode.Uri): Thenable<boolean> {
  return vscode.workspace.fs.stat(uri).then(
    () => true,
    () => false,
  );
}

function configuredRoot(): vscode.Uri | undefined {
  const value = vscode.workspace.getConfiguration('acm').get<string>('scripts.root', '').trim();
  const folder = workspaceFolder();
  if (!value || !folder) {
    return undefined;
  }
  return path.isAbsolute(value) ? vscode.Uri.file(value) : vscode.Uri.joinPath(folder.uri, value);
}

async function discoverRoots(): Promise<vscode.Uri[]> {
  const folder = workspaceFolder();
  if (!folder) {
    return [];
  }
  const files = await vscode.workspace.findFiles(ROOT_GLOB, IGNORED_GLOB, 5000);
  return scriptRootsOf(files.map((file) => file.path)).map((root) => folder.uri.with({ path: root }));
}

/** The setting wins; otherwise the only discovered root, or the one the user picked among several. */
async function resolveRoots(): Promise<Roots> {
  const configured = configuredRoot();
  if (configured) {
    return { root: configured, candidates: [] };
  }
  const candidates = await discoverRoots();
  const remembered = state.get<string>(ROOT_STATE_KEY);
  const root = candidates.length === 1 ? candidates[0] : candidates.find((candidate) => candidate.toString() === remembered);
  return { root, candidates };
}

async function listScripts(root: vscode.Uri): Promise<LocalScript[]> {
  const files = await vscode.workspace.findFiles(new vscode.RelativePattern(root, '**/*.groovy'), IGNORED_GLOB);
  return files.flatMap((uri) => {
    const id = scriptIdOf(uri.path.slice(root.path.length));
    const type = scriptTypeOf(id);
    return type ? [{ uri, id, type }] : [];
  });
}

class ProjectScriptsProvider implements vscode.TreeDataProvider<Node> {
  private readonly changed = new vscode.EventEmitter<void>();
  readonly onDidChangeTreeData = this.changed.event;
  roots: Roots = { candidates: [] };
  view?: vscode.TreeView<Node>;

  async refresh(): Promise<void> {
    this.roots = await resolveRoots();
    const { root, candidates } = this.roots;
    await vscode.commands.executeCommand('setContext', 'acm.hasScriptsRoot', !!root || candidates.length > 0);
    if (this.view) {
      this.view.description = root && vscode.workspace.asRelativePath(root).replace(/\/jcr_root\/conf\/acm\/settings\/script$/, '');
    }
    this.changed.fire();
  }

  async getChildren(node?: Node): Promise<Node[]> {
    if (node) {
      return node.kind === 'type' ? node.scripts.map((script) => ({ kind: 'script', script })) : [];
    }
    const { root } = this.roots;
    if (!root) {
      return [
        {
          kind: 'message',
          label: 'Select the scripts folder of the project',
          icon: 'folder',
          command: { title: 'Select Scripts Folder', command: 'acm.selectScriptsRoot' },
        },
      ];
    }
    const scripts = (await listScripts(root)).sort((a, b) => scriptLabel(a.id).localeCompare(scriptLabel(b.id)));
    return SCRIPT_TYPES.flatMap((type) => {
      const ofType = scripts.filter((script) => script.type === type);
      return ofType.length > 0 || ALWAYS_SHOWN.includes(type) ? [{ kind: 'type' as const, type, scripts: ofType }] : [];
    });
  }

  getTreeItem(node: Node): vscode.TreeItem {
    switch (node.kind) {
      case 'type': {
        const item = new vscode.TreeItem(
          node.type.charAt(0) + node.type.slice(1).toLowerCase(),
          vscode.TreeItemCollapsibleState.Expanded,
        );
        item.id = `project.${node.type}`;
        item.description = String(node.scripts.length);
        item.iconPath = vscode.ThemeIcon.Folder;
        item.contextValue = 'projectType';
        return item;
      }
      case 'script': {
        const item = new vscode.TreeItem(scriptLabel(node.script.id));
        item.tooltip = vscode.workspace.asRelativePath(node.script.uri);
        item.resourceUri = node.script.uri;
        item.contextValue = `projectScript.${node.script.type.toLowerCase()}`;
        item.command = { title: 'Open', command: 'vscode.open', arguments: [node.script.uri] };
        return item;
      }
      default: {
        const item = new vscode.TreeItem(node.label);
        item.iconPath = new vscode.ThemeIcon(node.icon);
        item.command = node.command;
        return item;
      }
    }
  }
}

async function selectRoot(): Promise<void> {
  if (configuredRoot()) {
    vscode.window.showInformationMessage('ACM: The scripts folder is set by acm.scripts.root.');
    return;
  }
  const candidates = await discoverRoots();
  if (candidates.length === 0) {
    const action = await vscode.window.showWarningMessage(
      'ACM: No scripts folder found in the workspace. Set acm.scripts.root to the project\'s jcr_root/conf/acm/settings/script.',
      'Open Settings',
    );
    if (action) {
      await vscode.commands.executeCommand('workbench.action.openSettings', 'acm.scripts.root');
    }
    return;
  }
  const picked = await vscode.window.showQuickPick(
    candidates.map((root) => ({ label: vscode.workspace.asRelativePath(root), root })),
    { title: 'ACM Scripts Folder', placeHolder: 'Select the folder with the project\'s ACM scripts' },
  );
  if (picked) {
    await state.update(ROOT_STATE_KEY, picked.root.toString());
    await provider.refresh();
  }
}

/** The scripts folder, asking to pick one when several are found; `undefined` when none or cancelled. */
async function requireRoot(): Promise<vscode.Uri | undefined> {
  if (!provider.roots.root) {
    await selectRoot();
  }
  return provider.roots.root;
}

async function newScript(node?: Node): Promise<void> {
  const root = await requireRoot();
  if (!root) {
    return;
  }
  const type =
    node?.kind === 'type'
      ? node.type
      : (
          await vscode.window.showQuickPick(
            SCRIPT_TYPES.map((candidate) => ({ label: candidate, type: candidate })),
            { title: 'New ACM Script', placeHolder: 'Script type' },
          )
        )?.type;
  if (!type) {
    return;
  }
  const template = await vscode.window.showQuickPick(
    SCRIPT_TEMPLATES.map((candidate) => ({ label: candidate.name, detail: candidate.description, candidate })),
    { title: `New ${type.toLowerCase()} script`, placeHolder: 'Select ACM script template', matchOnDetail: true },
  );
  if (!template) {
    return;
  }
  const name = await vscode.window.showInputBox({
    title: `New ${type.toLowerCase()} script`,
    prompt: 'Name or path under the type folder, e.g. example/ACME-1_hello',
    validateInput: validateScriptName,
  });
  if (name === undefined) {
    return;
  }
  const folder = vscode.Uri.joinPath(root, type.toLowerCase());
  const file = vscode.Uri.joinPath(folder, `${name.trim().replace(/\.groovy$/, '')}.groovy`);
  if (!file.path.startsWith(`${folder.path}/`)) {
    throw new Error(`Script path "${name}" is outside the ${type.toLowerCase()} folder.`);
  }
  if (await fileExists(file)) {
    throw new Error(`Script ${vscode.workspace.asRelativePath(file)} already exists.`);
  }
  await vscode.workspace.fs.createDirectory(vscode.Uri.joinPath(file, '..'));
  await vscode.workspace.fs.writeFile(file, new TextEncoder().encode(template.candidate.code));
  await vscode.window.showTextDocument(await vscode.workspace.openTextDocument(file));
  await provider.refresh();
}

async function runScript(node?: Node): Promise<void> {
  if (node?.kind === 'script') {
    await vscode.window.showTextDocument(node.script.uri);
    await vscode.commands.executeCommand('acm.run');
  }
}

async function showDiff(instance: AcmInstance, id: string, local: vscode.Uri): Promise<void> {
  await vscode.commands.executeCommand(
    'vscode.diff',
    scriptUri(instance, id),
    local,
    `${scriptLabel(id)} (${instance.name}) \u2194 ${vscode.workspace.asRelativePath(local)}`,
  );
}

/** Compares a script stored on the instance with its local file in the project, or one the user picks. */
async function compareInstanceScript(node?: { instance?: AcmInstance; script?: { id: string } }): Promise<void> {
  if (!node?.instance || !node.script) {
    return;
  }
  const { id } = node.script;
  const { root } = provider.roots;
  const mapped = root && id.startsWith(SCRIPT_ROOT) ? vscode.Uri.joinPath(root, id.slice(SCRIPT_ROOT.length)) : undefined;
  let local = mapped && (await fileExists(mapped)) ? mapped : undefined;
  if (!local) {
    const name = path.posix.basename(id);
    const files = await vscode.workspace.findFiles(`**/${name}`, IGNORED_GLOB, 50);
    local =
      files.length === 1
        ? files[0]
        : files.length > 1
          ? (
              await vscode.window.showQuickPick(
                files.map((uri) => ({ label: vscode.workspace.asRelativePath(uri), uri })),
                { placeHolder: `Local file to compare with ${name}` },
              )
            )?.uri
          : (await vscode.window.showOpenDialog({ canSelectMany: false, filters: { Groovy: ['groovy'] } }))?.[0];
  }
  if (local) {
    await showDiff(node.instance, id, local);
  }
}

/** Compares a project script with the version on the active instance; says so when it is not deployed there. */
async function compareProjectScript(node?: Node): Promise<void> {
  if (node?.kind !== 'script') {
    return;
  }
  const target = await getTarget();
  if (!target) {
    return;
  }
  const { id, uri } = node.script;
  const deployed = await target.client
    .request<{ list?: unknown[] }>('GET', `${ACM_API.script}?id=${encodeURIComponent(id)}`)
    .then(
      (res) => (res.data?.list?.length ?? 0) > 0,
      (error: unknown) => {
        if (error instanceof AcmHttpError && error.httpStatus === 404) {
          return false;
        }
        throw error;
      },
    );
  if (!deployed) {
    vscode.window.showInformationMessage(`ACM: ${scriptLabel(id)} is not on instance "${target.instance.name}" yet.`);
    return;
  }
  await showDiff(target.instance, id, uri);
}

export function registerProjectScripts(context: vscode.ExtensionContext): void {
  state = context.workspaceState;
  provider = new ProjectScriptsProvider();
  provider.view = vscode.window.createTreeView('acm.projectScripts', { treeDataProvider: provider });
  let timer: ReturnType<typeof setTimeout> | undefined;
  const refreshSoon = () => {
    clearTimeout(timer);
    timer = setTimeout(() => void provider.refresh(), 300);
  };
  const onScriptFile = (uri: vscode.Uri) => {
    const root = provider.roots.root;
    if (uri.path.includes('/jcr_root/conf/acm/settings/script/') || (root && uri.path.startsWith(`${root.path}/`))) {
      refreshSoon();
    }
  };
  const watcher = vscode.workspace.createFileSystemWatcher('**/*.groovy', false, true, false);
  context.subscriptions.push(
    provider.view,
    watcher,
    watcher.onDidCreate(onScriptFile),
    watcher.onDidDelete(onScriptFile),
    { dispose: () => clearTimeout(timer) },
    vscode.workspace.onDidChangeWorkspaceFolders(refreshSoon),
    vscode.workspace.onDidChangeConfiguration((event) => {
      if (event.affectsConfiguration('acm.scripts.root')) {
        refreshSoon();
      }
    }),
    registerCommand('acm.selectScriptsRoot', selectRoot),
    registerCommand('acm.newProjectScript', newScript),
    registerCommand('acm.runProjectScript', runScript),
    registerCommand('acm.compareProjectScript', compareProjectScript),
    registerCommand('acm.compareScript', compareInstanceScript),
  );
  void provider.refresh();
}
