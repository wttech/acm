import * as path from 'node:path';
import * as vscode from 'vscode';
import {
  ACM_API,
  AcmHttpError,
  enabledScriptTypes,
  PACKAGE_SCRIPT_ROOT,
  SCRIPT_ROOT,
  SCRIPT_TYPE_INFO,
  SNIPPET_EXTENSION,
  scriptIdOf,
  scriptLabel,
  scriptRootLabels,
  scriptRootsOf,
  scriptTypeOf,
  snippetTemplate,
  templatesFor,
  validateRelativeName,
  type ScriptType,
} from '@acm/shared';
import { registerCommand } from './errors';
import { COMMANDS, CONTEXT, ITEMS, SETTINGS, VIEWS, settingId } from './ids';
import { getTarget, readSetting, type AcmInstance } from './instances';
import { isMockEnabled, onDidChangeMock } from './mock';
import { scriptUri } from './views';

const ROOT_GLOB = `**${PACKAGE_SCRIPT_ROOT}/**`;
const IGNORED_GLOB = '**/{node_modules,target,.git}/**';
const SCRIPT_EXTENSION = '.groovy';

interface LocalScript {
  uri: vscode.Uri;
  id: string;
  type: ScriptType;
  root: vscode.Uri;
}

interface LocalSnippet {
  uri: vscode.Uri;
  label: string;
  root: vscode.Uri;
}

interface RootContent {
  scripts: LocalScript[];
  snippets: LocalSnippet[];
}

type Node =
  | { kind: 'root'; root: vscode.Uri; label: string; content: RootContent }
  | { kind: 'type'; root: vscode.Uri; type: ScriptType; scripts: LocalScript[] }
  | { kind: 'script'; script: LocalScript }
  | { kind: 'snippets'; root: vscode.Uri; snippets: LocalSnippet[] }
  | { kind: 'snippet'; snippet: LocalSnippet };

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
  const files = await vscode.workspace.findFiles(new vscode.RelativePattern(root, `**/*${SCRIPT_EXTENSION}`), IGNORED_GLOB);
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
function rootChildren(root: vscode.Uri, { scripts, snippets }: RootContent): Node[] {
  const types = enabledScriptTypes({ mock: isMockEnabled() }).map((type) => ({
    kind: 'type' as const,
    root,
    type,
    scripts: scripts.filter((script) => script.type === type),
  }));
  return [...types, { kind: 'snippets', root, snippets }];
}

/** Snippets live next to the scripts folder: `conf/acm/settings/snippet/available`. */
function snippetsFolder(root: vscode.Uri): vscode.Uri {
  return vscode.Uri.joinPath(root, '..', 'snippet', 'available');
}

function scriptFolder(root: vscode.Uri, type: ScriptType): vscode.Uri {
  return vscode.Uri.joinPath(root, type.toLowerCase());
}

async function listSnippets(root: vscode.Uri): Promise<LocalSnippet[]> {
  const folder = snippetsFolder(root);
  const files = await vscode.workspace.findFiles(new vscode.RelativePattern(folder, `**/*${SNIPPET_EXTENSION}`), IGNORED_GLOB);
  return files
    .map((uri) => ({ uri, label: stripExtension(uri.path.slice(folder.path.length + 1), SNIPPET_EXTENSION), root }))
    .sort((a, b) => a.label.localeCompare(b.label));
}

async function listContent(root: vscode.Uri): Promise<RootContent> {
  const [scripts, snippets] = await Promise.all([listScripts(root), listSnippets(root)]);
  return { scripts, snippets };
}

function stripExtension(name: string, extension: string): string {
  return name.endsWith(extension) ? name.slice(0, -extension.length) : name;
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
      switch (node.kind) {
        case 'root':
          return rootChildren(node.root, node.content);
        case 'type':
          return node.scripts.map((script) => ({ kind: 'script', script }));
        case 'snippets':
          return node.snippets.map((snippet) => ({ kind: 'snippet', snippet }));
        default:
          return [];
      }
    }
    const { roots } = this;
    const contents = await Promise.all(roots.map(listContent));
    if (roots.length === 1) {
      return rootChildren(roots[0], contents[0]);
    }
    return roots.map((root, index) => this.rootNode(root, contents[index]));
  }

  private rootNode(root: vscode.Uri, content: RootContent): Node {
    const labels = scriptRootLabels(this.roots.map((candidate) => candidate.path));
    const index = this.roots.findIndex((candidate) => candidate.toString() === root.toString());
    return { kind: 'root', root, label: labels[index], content };
  }

  async getParent(node: Node): Promise<Node | undefined> {
    if (node.kind === 'root') {
      return undefined;
    }
    const root = node.kind === 'script' ? node.script.root : node.kind === 'snippet' ? node.snippet.root : node.root;
    const content = await listContent(root);
    if (node.kind === 'script') {
      const { type } = node.script;
      return { kind: 'type', root, type, scripts: content.scripts.filter((script) => script.type === type) };
    }
    if (node.kind === 'snippet') {
      return { kind: 'snippets', root, snippets: content.snippets };
    }
    return this.roots.length > 1 ? this.rootNode(root, content) : undefined;
  }

  /** Selects a script or snippet in the tree, leaving the focus in the editor. */
  async reveal(uri: vscode.Uri): Promise<void> {
    const contents = await Promise.all(this.roots.map(listContent));
    const script = contents.flatMap((content) => content.scripts).find((candidate) => candidate.uri.toString() === uri.toString());
    const snippet = contents.flatMap((content) => content.snippets).find((candidate) => candidate.uri.toString() === uri.toString());
    const node: Node | undefined = script ? { kind: 'script', script } : snippet ? { kind: 'snippet', snippet } : undefined;
    if (node && this.view) {
      await this.view.reveal(node, { select: true });
    }
  }

  getTreeItem(node: Node): vscode.TreeItem {
    switch (node.kind) {
      case 'root': {
        const item = new vscode.TreeItem(node.label, vscode.TreeItemCollapsibleState.Expanded);
        item.id = `project.${node.root.toString()}`;
        item.description = String(node.content.scripts.length + node.content.snippets.length);
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
      case 'snippets': {
        const item = new vscode.TreeItem('Snippets', vscode.TreeItemCollapsibleState.Expanded);
        item.id = `project.${node.root.toString()}.snippets`;
        item.description = String(node.snippets.length);
        item.tooltip = 'Code templates offered in the ACM Console and the Snippets page: YAML files in conf/acm/settings/snippet/available.';
        item.iconPath = new vscode.ThemeIcon('symbol-snippet');
        item.contextValue = ITEMS.projectSnippets;
        return item;
      }
      case 'snippet': {
        const item = new vscode.TreeItem(node.snippet.label);
        item.id = `project.${node.snippet.uri.toString()}`;
        item.tooltip = vscode.workspace.asRelativePath(node.snippet.uri);
        item.resourceUri = node.snippet.uri;
        item.contextValue = ITEMS.projectSnippet;
        item.command = { title: 'Open', command: 'vscode.open', arguments: [node.snippet.uri] };
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

/** A new file location for a name under a folder; fails when it escapes the folder or is taken. */
async function freeFile(folder: vscode.Uri, extension: string, name: string): Promise<vscode.Uri> {
  const file = vscode.Uri.joinPath(folder, `${stripExtension(name.trim(), extension)}${extension}`);
  if (!file.path.startsWith(`${folder.path}/`)) {
    throw new Error(`Path "${name}" is outside ${vscode.workspace.asRelativePath(folder)}.`);
  }
  if (await fileExists(file)) {
    throw new Error(`${vscode.workspace.asRelativePath(file)} already exists.`);
  }
  await vscode.workspace.fs.createDirectory(vscode.Uri.joinPath(file, '..'));
  return file;
}

/** The file of a script or snippet node with the folder its name is relative to. */
function fileOf(node?: Node): { uri: vscode.Uri; folder: vscode.Uri; extension: string; label: string } | undefined {
  if (node?.kind === 'script') {
    const { uri, id, type, root } = node.script;
    return { uri, folder: scriptFolder(root, type), extension: SCRIPT_EXTENSION, label: scriptLabel(id) };
  }
  if (node?.kind === 'snippet') {
    const { uri, label, root } = node.snippet;
    return { uri, folder: snippetsFolder(root), extension: SNIPPET_EXTENSION, label };
  }
  return undefined;
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
    validateInput: (value) => validateRelativeName(value, SCRIPT_EXTENSION),
  });
  if (name === undefined) {
    return;
  }
  const file = await freeFile(scriptFolder(root, type), SCRIPT_EXTENSION, name);
  await vscode.workspace.fs.writeFile(file, new TextEncoder().encode(template.candidate.code));
  await vscode.window.showTextDocument(await vscode.workspace.openTextDocument(file));
  await provider.refresh();
  await provider.reveal(file);
}

async function newSnippet(node?: Node): Promise<void> {
  if (node?.kind !== 'snippets') {
    return;
  }
  const name = await vscode.window.showInputBox({
    title: 'New snippet',
    prompt: 'Name or path under the snippets folder, e.g. acme/hello',
    validateInput: (value) => validateRelativeName(value, SNIPPET_EXTENSION),
  });
  if (name === undefined) {
    return;
  }
  const file = await freeFile(snippetsFolder(node.root), SNIPPET_EXTENSION, name);
  await vscode.workspace.fs.writeFile(file, new TextEncoder().encode(snippetTemplate(name)));
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

async function renameFile(node?: Node): Promise<void> {
  const file = fileOf(node);
  if (!file) {
    return;
  }
  const current = stripExtension(file.uri.path.slice(file.folder.path.length + 1), file.extension);
  const name = await vscode.window.showInputBox({
    title: `Rename ${file.label}`,
    prompt: 'Name or path under the folder',
    value: current,
    validateInput: (value) => validateRelativeName(value, file.extension),
  });
  if (name === undefined || stripExtension(name.trim(), file.extension) === current) {
    return;
  }
  const target = await freeFile(file.folder, file.extension, name);
  const edit = new vscode.WorkspaceEdit();
  edit.renameFile(file.uri, target);
  if (!(await vscode.workspace.applyEdit(edit))) {
    throw new Error(`Cannot rename ${vscode.workspace.asRelativePath(file.uri)}.`);
  }
  await provider.refresh();
  await provider.reveal(target);
}

async function duplicateFile(node?: Node): Promise<void> {
  const file = fileOf(node);
  if (!file) {
    return;
  }
  const current = stripExtension(file.uri.path.slice(file.folder.path.length + 1), file.extension);
  const name = await vscode.window.showInputBox({
    title: `Duplicate ${file.label}`,
    prompt: 'Name or path of the copy under the folder',
    value: `${current}_copy`,
    validateInput: (value) => validateRelativeName(value, file.extension),
  });
  if (name === undefined) {
    return;
  }
  const target = await freeFile(file.folder, file.extension, name);
  await vscode.workspace.fs.copy(file.uri, target);
  await vscode.window.showTextDocument(await vscode.workspace.openTextDocument(target));
  await provider.refresh();
  await provider.reveal(target);
}

async function deleteFile(node?: Node): Promise<void> {
  const file = fileOf(node);
  if (!file) {
    return;
  }
  const action = await vscode.window.showWarningMessage(
    `Delete ${file.label}?`,
    { modal: true, detail: 'The file is moved to the trash. A copy deployed to an instance is not affected.' },
    'Delete',
  );
  if (action) {
    await vscode.workspace.fs.delete(file.uri, { useTrash: true });
    await provider.refresh();
  }
}

async function revealFile(node?: Node): Promise<void> {
  const file = fileOf(node);
  if (file) {
    await vscode.commands.executeCommand('revealInExplorer', file.uri);
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
  const onProjectFile = (uri: vscode.Uri) => {
    if (
      uri.path.includes(`${PACKAGE_SCRIPT_ROOT}/`) ||
      provider.roots.some(
        (root) => uri.path.startsWith(`${root.path}/`) || uri.path.startsWith(`${snippetsFolder(root).path}/`),
      )
    ) {
      refreshSoon();
    }
  };
  const watcher = vscode.workspace.createFileSystemWatcher(`**/*{${SCRIPT_EXTENSION},${SNIPPET_EXTENSION}}`, false, true, false);
  context.subscriptions.push(
    provider.view,
    watcher,
    watcher.onDidCreate(onProjectFile),
    watcher.onDidDelete(onProjectFile),
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
    registerCommand(COMMANDS.newProjectSnippet, newSnippet),
    registerCommand(COMMANDS.runProjectScript, runScript),
    registerCommand(COMMANDS.renameProjectFile, renameFile),
    registerCommand(COMMANDS.duplicateProjectFile, duplicateFile),
    registerCommand(COMMANDS.deleteProjectFile, deleteFile),
    registerCommand(COMMANDS.revealProjectFile, revealFile),
    registerCommand(COMMANDS.compareProjectScript, compareProjectScript),
    registerCommand(COMMANDS.compareScript, compareInstanceScript),
  );
  void provider.refresh();
}
