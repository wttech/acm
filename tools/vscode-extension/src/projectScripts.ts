import * as path from 'node:path';
import * as vscode from 'vscode';
import {
  ACM_API,
  AcmHttpError,
  enabledScriptTypes,
  PACKAGE_SCRIPT_ROOT,
  SCRIPT_ROOT,
  SCRIPT_TYPE_INFO,
  scriptIdOf,
  scriptLabel,
  scriptRootLabels,
  scriptRootsOf,
  scriptTypeOf,
  templatesFor,
  validateScriptName,
  type ScriptType,
} from '@acm/shared';
import { registerCommand } from './errors';
import { COMMANDS, CONTEXT, ITEMS, SETTINGS, VIEWS, settingId } from './ids';
import { getTarget, readSetting, type AcmInstance } from './instances';
import { isMockEnabled, onDidChangeMock } from './mock';
import { scriptUri } from './views';

const ROOT_GLOB = `**${PACKAGE_SCRIPT_ROOT}/**`;
const IGNORED_GLOB = '**/{node_modules,target,.git}/**';

interface LocalScript {
  uri: vscode.Uri;
  id: string;
  type: ScriptType;
  root: vscode.Uri;
}

type Node =
  | { kind: 'root'; root: vscode.Uri; label: string; scripts: LocalScript[] }
  | { kind: 'type'; root: vscode.Uri; type: ScriptType; scripts: LocalScript[] }
  | { kind: 'script'; script: LocalScript };

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

async function firstExisting(uris: vscode.Uri[]): Promise<vscode.Uri | undefined> {
  for (const uri of uris) {
    if (await fileExists(uri)) {
      return uri;
    }
  }
  return undefined;
}

function configuredRoots(): vscode.Uri[] {
  const folder = workspaceFolder();
  if (!folder) {
    return [];
  }
  return readSetting<string[]>(SETTINGS.scriptsRoots).map((value) =>
    path.isAbsolute(value) ? vscode.Uri.file(value) : vscode.Uri.joinPath(folder.uri, value),
  );
}

function rootDescription(root: vscode.Uri): string {
  return vscode.workspace.asRelativePath(root).replace(new RegExp(`${PACKAGE_SCRIPT_ROOT}$`), '');
}

async function discoverRoots(): Promise<vscode.Uri[]> {
  const folder = workspaceFolder();
  if (!folder) {
    return [];
  }
  const files = await vscode.workspace.findFiles(ROOT_GLOB, IGNORED_GLOB, 5000);
  return scriptRootsOf(files.map((file) => file.path)).map((root) => folder.uri.with({ path: root }));
}

/** The configured roots, or else the ones discovered in the workspace. */
async function resolveRoots(): Promise<vscode.Uri[]> {
  const configured = configuredRoots();
  return configured.length > 0 ? configured : discoverRoots();
}

async function listScripts(root: vscode.Uri): Promise<LocalScript[]> {
  const files = await vscode.workspace.findFiles(new vscode.RelativePattern(root, '**/*.groovy'), IGNORED_GLOB);
  const visible = enabledScriptTypes({ mock: isMockEnabled() });
  return files
    .flatMap((uri) => {
      const id = scriptIdOf(uri.path.slice(root.path.length));
      const type = scriptTypeOf(id);
      return type && visible.includes(type) ? [{ uri, id, type, root }] : [];
    })
    .sort((a, b) => scriptLabel(a.id).localeCompare(scriptLabel(b.id)));
}

/** Every enabled type is listed, also when empty or without a folder yet, so a script can be added to it. */
function typeNodes(root: vscode.Uri, scripts: LocalScript[]): Node[] {
  return enabledScriptTypes({ mock: isMockEnabled() }).map((type) => ({
    kind: 'type',
    root,
    type,
    scripts: scripts.filter((script) => script.type === type),
  }));
}

class ProjectScriptsProvider implements vscode.TreeDataProvider<Node> {
  private readonly changed = new vscode.EventEmitter<void>();
  readonly onDidChangeTreeData = this.changed.event;
  roots: vscode.Uri[] = [];
  view?: vscode.TreeView<Node>;

  async refresh(): Promise<void> {
    this.roots = await resolveRoots();
    await vscode.commands.executeCommand('setContext', CONTEXT.hasScriptsRoot, this.roots.length > 0);
    if (this.view) {
      this.view.description = this.roots.length === 1 ? rootDescription(this.roots[0]) : undefined;
    }
    this.changed.fire();
  }

  async getChildren(node?: Node): Promise<Node[]> {
    if (node) {
      if (node.kind === 'root') {
        return typeNodes(node.root, node.scripts);
      }
      return node.kind === 'type' ? node.scripts.map((script) => ({ kind: 'script', script })) : [];
    }
    const { roots } = this;
    const scripts = await Promise.all(roots.map(listScripts));
    if (roots.length === 1) {
      return typeNodes(roots[0], scripts[0]);
    }
    return roots.map((root, index) => this.rootNode(root, scripts[index]));
  }

  private rootNode(root: vscode.Uri, scripts: LocalScript[]): Node {
    const labels = scriptRootLabels(this.roots.map((candidate) => candidate.path));
    const index = this.roots.findIndex((candidate) => candidate.toString() === root.toString());
    return { kind: 'root', root, label: labels[index], scripts };
  }

  async getParent(node: Node): Promise<Node | undefined> {
    if (node.kind === 'root') {
      return undefined;
    }
    const root = node.kind === 'type' ? node.root : node.script.root;
    const scripts = await listScripts(root);
    if (node.kind === 'type') {
      return this.roots.length > 1 ? this.rootNode(root, scripts) : undefined;
    }
    const { type } = node.script;
    return { kind: 'type', root, type, scripts: scripts.filter((script) => script.type === type) };
  }

  /** Selects a script in the tree, leaving the focus in the editor. */
  async reveal(uri: vscode.Uri): Promise<void> {
    const scripts = (await Promise.all(this.roots.map(listScripts))).flat();
    const script = scripts.find((candidate) => candidate.uri.toString() === uri.toString());
    if (script && this.view) {
      await this.view.reveal({ kind: 'script', script }, { select: true });
    }
  }

  getTreeItem(node: Node): vscode.TreeItem {
    switch (node.kind) {
      case 'root': {
        const item = new vscode.TreeItem(node.label, vscode.TreeItemCollapsibleState.Expanded);
        item.id = `project.${node.root.toString()}`;
        item.description = String(node.scripts.length);
        item.tooltip = rootDescription(node.root);
        item.iconPath = new vscode.ThemeIcon('root-folder');
        item.contextValue = ITEMS.projectRoot;
        return item;
      }
      case 'type': {
        const item = new vscode.TreeItem(SCRIPT_TYPE_INFO[node.type].label, vscode.TreeItemCollapsibleState.Expanded);
        item.tooltip = SCRIPT_TYPE_INFO[node.type].description;
        item.id = `project.${node.root.toString()}.${node.type}`;
        item.description = String(node.scripts.length);
        item.iconPath = vscode.ThemeIcon.Folder;
        item.contextValue = ITEMS.projectType;
        return item;
      }
      case 'script': {
        const item = new vscode.TreeItem(scriptLabel(node.script.id));
        item.id = `project.${node.script.uri.toString()}`;
        item.tooltip = vscode.workspace.asRelativePath(node.script.uri);
        item.resourceUri = node.script.uri;
        item.contextValue = ITEMS.projectScript(node.script.type);
        item.command = { title: 'Open', command: 'vscode.open', arguments: [node.script.uri] };
        return item;
      }
    }
  }
}

/** The only scripts folder, else the one the user picks; `undefined` when none or cancelled. */
async function requireRoot(): Promise<vscode.Uri | undefined> {
  const { roots } = provider;
  if (roots.length === 0) {
    const action = await vscode.window.showWarningMessage(
      `ACM: No scripts folder found in the workspace. Set ${settingId(SETTINGS.scriptsRoots)} to the project's jcr_root/conf/acm/settings/script.`,
      'Open Settings',
    );
    if (action) {
      await vscode.commands.executeCommand('workbench.action.openSettings', settingId(SETTINGS.scriptsRoots));
    }
    return undefined;
  }
  if (roots.length === 1) {
    return roots[0];
  }
  const labels = scriptRootLabels(roots.map((root) => root.path));
  const picked = await vscode.window.showQuickPick(
    roots.map((root, index) => ({ label: labels[index], description: rootDescription(root), root })),
    { title: 'ACM Scripts Folder', placeHolder: "Select the project's folder with ACM scripts" },
  );
  return picked?.root;
}

/** A new file location for a script name under its type folder; fails when it escapes the folder or is taken. */
async function freeScriptFile(root: vscode.Uri, type: ScriptType, name: string): Promise<vscode.Uri> {
  const folder = vscode.Uri.joinPath(root, type.toLowerCase());
  const file = vscode.Uri.joinPath(folder, `${name.trim().replace(/\.groovy$/, '')}.groovy`);
  if (!file.path.startsWith(`${folder.path}/`)) {
    throw new Error(`Script path "${name}" is outside the ${type.toLowerCase()} folder.`);
  }
  if (await fileExists(file)) {
    throw new Error(`Script ${vscode.workspace.asRelativePath(file)} already exists.`);
  }
  await vscode.workspace.fs.createDirectory(vscode.Uri.joinPath(file, '..'));
  return file;
}

/** From the view title or the palette: VS Code passes the selected item there, so it is ignored and the folder and type are asked. */
async function newScript(): Promise<void> {
  await createScript();
}

async function newScriptOfType(node?: Node): Promise<void> {
  if (node?.kind === 'type') {
    await createScript(node.root, node.type);
  }
}

async function createScript(knownRoot?: vscode.Uri, knownType?: ScriptType): Promise<void> {
  const root = knownRoot ?? (await requireRoot());
  if (!root) {
    return;
  }
  const type =
    knownType ??
    (
      await vscode.window.showQuickPick(
        enabledScriptTypes({ mock: isMockEnabled() }).map((candidate) => ({
          label: SCRIPT_TYPE_INFO[candidate].label,
          detail: SCRIPT_TYPE_INFO[candidate].description,
          type: candidate,
        })),
        { title: 'New ACM Script', placeHolder: 'Script type', matchOnDetail: true },
      )
    )?.type;
  if (!type) {
    return;
  }
  const template = await vscode.window.showQuickPick(
    templatesFor(type).map((candidate) => ({ label: candidate.name, detail: candidate.description, candidate })),
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
  const file = await freeScriptFile(root, type, name);
  await vscode.workspace.fs.writeFile(file, new TextEncoder().encode(template.candidate.code));
  await vscode.window.showTextDocument(await vscode.workspace.openTextDocument(file));
  await provider.refresh();
  await provider.reveal(file);
}

async function runScript(node?: Node): Promise<void> {
  if (node?.kind === 'script') {
    await vscode.window.showTextDocument(node.script.uri);
    await vscode.commands.executeCommand(COMMANDS.run);
  }
}

async function renameScript(node?: Node): Promise<void> {
  if (node?.kind !== 'script') {
    return;
  }
  const { id, uri, type, root } = node.script;
  const folder = vscode.Uri.joinPath(root, type.toLowerCase());
  const current = uri.path.slice(folder.path.length + 1).replace(/\.groovy$/, '');
  const name = await vscode.window.showInputBox({
    title: `Rename ${scriptLabel(id)}`,
    prompt: 'Name or path under the type folder',
    value: current,
    validateInput: validateScriptName,
  });
  if (name === undefined || name.trim().replace(/\.groovy$/, '') === current) {
    return;
  }
  const target = await freeScriptFile(root, type, name);
  const edit = new vscode.WorkspaceEdit();
  edit.renameFile(uri, target);
  if (!(await vscode.workspace.applyEdit(edit))) {
    throw new Error(`Cannot rename ${vscode.workspace.asRelativePath(uri)}.`);
  }
  await provider.refresh();
  await provider.reveal(target);
}

async function duplicateScript(node?: Node): Promise<void> {
  if (node?.kind !== 'script') {
    return;
  }
  const { id, uri, type, root } = node.script;
  const folder = vscode.Uri.joinPath(root, type.toLowerCase());
  const current = uri.path.slice(folder.path.length + 1).replace(/\.groovy$/, '');
  const name = await vscode.window.showInputBox({
    title: `Duplicate ${scriptLabel(id)}`,
    prompt: 'Name or path of the copy under the type folder',
    value: `${current}_copy`,
    validateInput: validateScriptName,
  });
  if (name === undefined) {
    return;
  }
  const target = await freeScriptFile(root, type, name);
  await vscode.workspace.fs.copy(uri, target);
  await vscode.window.showTextDocument(await vscode.workspace.openTextDocument(target));
  await provider.refresh();
  await provider.reveal(target);
}

async function deleteScript(node?: Node): Promise<void> {
  if (node?.kind !== 'script') {
    return;
  }
  const action = await vscode.window.showWarningMessage(
    `Delete ${scriptLabel(node.script.id)}?`,
    { modal: true, detail: 'The file is moved to the trash. A copy deployed to an instance is not affected.' },
    'Delete',
  );
  if (action) {
    await vscode.workspace.fs.delete(node.script.uri, { useTrash: true });
    await provider.refresh();
  }
}

async function revealScript(node?: Node): Promise<void> {
  if (node?.kind === 'script') {
    await vscode.commands.executeCommand('revealInExplorer', node.script.uri);
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
  const relative = id.startsWith(SCRIPT_ROOT) ? id.slice(SCRIPT_ROOT.length) : undefined;
  let local =
    relative === undefined ? undefined : await firstExisting(provider.roots.map((root) => vscode.Uri.joinPath(root, relative)));
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
  provider = new ProjectScriptsProvider();
  provider.view = vscode.window.createTreeView(VIEWS.projectScripts, { treeDataProvider: provider });
  let timer: ReturnType<typeof setTimeout> | undefined;
  const refreshSoon = () => {
    clearTimeout(timer);
    timer = setTimeout(() => void provider.refresh(), 300);
  };
  const onScriptFile = (uri: vscode.Uri) => {
    if (uri.path.includes(`${PACKAGE_SCRIPT_ROOT}/`) || provider.roots.some((root) => uri.path.startsWith(`${root.path}/`))) {
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
    onDidChangeMock(refreshSoon),
    vscode.workspace.onDidChangeConfiguration((event) => {
      if (event.affectsConfiguration(settingId(SETTINGS.scriptsRoots))) {
        refreshSoon();
      }
    }),
    registerCommand(COMMANDS.newProjectScript, newScript),
    registerCommand(COMMANDS.refreshProjectScripts, () => provider.refresh()),
    registerCommand(COMMANDS.newProjectScriptOfType, newScriptOfType),
    registerCommand(COMMANDS.runProjectScript, runScript),
    registerCommand(COMMANDS.renameProjectScript, renameScript),
    registerCommand(COMMANDS.duplicateProjectScript, duplicateScript),
    registerCommand(COMMANDS.deleteProjectScript, deleteScript),
    registerCommand(COMMANDS.revealProjectScript, revealScript),
    registerCommand(COMMANDS.compareProjectScript, compareProjectScript),
    registerCommand(COMMANDS.compareScript, compareInstanceScript),
  );
  void provider.refresh();
}
