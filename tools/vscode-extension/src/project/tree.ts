import * as vscode from 'vscode';
import { SCRIPT_TYPE_INFO } from '@acm/shared';
import { CONTEXT, ITEMS, VIEWS } from '../ids';
import {
  filesIn,
  listFiles,
  sectionKey,
  sectionOf,
  sections,
  type ProjectFile,
  type ProjectScript,
  type Section,
} from './files';
import { resolveRoots, rootDescription, rootLabels } from './roots';

export type Node =
  | { kind: 'root'; root: vscode.Uri; label: string; files: ProjectFile[] }
  | { kind: 'group'; root: vscode.Uri; section: Section; files: ProjectFile[] }
  | { kind: 'file'; file: ProjectFile };

export function fileOf(node?: Node): ProjectFile | undefined {
  return node?.kind === 'file' ? node.file : undefined;
}

export function scriptOf(node?: Node): ProjectScript | undefined {
  const file = fileOf(node);
  return file?.kind === 'script' ? file : undefined;
}

function describeSection(section: Section): { label: string; tooltip: string; icon: vscode.ThemeIcon; contextValue: string } {
  if (section.kind === 'scriptType') {
    const { label, description } = SCRIPT_TYPE_INFO[section.scriptType];
    return { label, tooltip: description, icon: vscode.ThemeIcon.Folder, contextValue: ITEMS.projectScriptType };
  }
  return {
    label: 'Snippets',
    tooltip: 'Code templates offered in the ACM Console and the Snippets page: YAML files in conf/acm/settings/snippet/available.',
    icon: new vscode.ThemeIcon('symbol-snippet'),
    contextValue: ITEMS.projectSnippets,
  };
}

function groups(root: vscode.Uri, files: ProjectFile[]): Node[] {
  return sections().map((section): Node => ({ kind: 'group', root, section, files: filesIn(files, section) }));
}

/** The project content: the scripts and snippets of the project's content packages. */
export class ProjectTree implements vscode.TreeDataProvider<Node> {
  private readonly changed = new vscode.EventEmitter<void>();
  readonly onDidChangeTreeData = this.changed.event;
  readonly view = vscode.window.createTreeView(VIEWS.projectContent, { treeDataProvider: this });
  roots: vscode.Uri[] = [];

  async refresh(): Promise<void> {
    this.roots = await resolveRoots();
    await vscode.commands.executeCommand('setContext', CONTEXT.hasProjectContent, this.roots.length > 0);
    this.view.description = this.roots.length === 1 ? rootDescription(this.roots[0]) : undefined;
    this.changed.fire();
  }

  async getChildren(node?: Node): Promise<Node[]> {
    if (!node) {
      const lists = await Promise.all(this.roots.map(listFiles));
      return this.roots.length === 1
        ? groups(this.roots[0], lists[0])
        : this.roots.map((root, index) => this.rootNode(root, lists[index]));
    }
    switch (node.kind) {
      case 'root':
        return groups(node.root, node.files);
      case 'group':
        return node.files.map((file): Node => ({ kind: 'file', file }));
      case 'file':
        return [];
    }
  }

  async getParent(node: Node): Promise<Node | undefined> {
    switch (node.kind) {
      case 'root':
        return undefined;
      case 'group':
        return this.roots.length > 1 ? this.rootNode(node.root, await listFiles(node.root)) : undefined;
      case 'file': {
        const { root } = node.file;
        const section = sectionOf(node.file);
        return { kind: 'group', root, section, files: filesIn(await listFiles(root), section) };
      }
    }
  }

  /** Selects a file in the tree, leaving the focus in the editor. */
  async reveal(uri: vscode.Uri): Promise<void> {
    const files = (await Promise.all(this.roots.map(listFiles))).flat();
    const file = files.find((candidate) => candidate.uri.toString() === uri.toString());
    if (file) {
      await this.view.reveal({ kind: 'file', file }, { select: true });
    }
  }

  getTreeItem(node: Node): vscode.TreeItem {
    switch (node.kind) {
      case 'root': {
        const item = new vscode.TreeItem(node.label, vscode.TreeItemCollapsibleState.Expanded);
        item.id = `project.${node.root.toString()}`;
        item.description = String(node.files.length);
        item.tooltip = rootDescription(node.root);
        item.iconPath = new vscode.ThemeIcon('root-folder');
        item.contextValue = ITEMS.projectRoot;
        return item;
      }
      case 'group': {
        const { label, tooltip, icon, contextValue } = describeSection(node.section);
        const item = new vscode.TreeItem(label, vscode.TreeItemCollapsibleState.Expanded);
        item.id = `project.${node.root.toString()}.${sectionKey(node.section)}`;
        item.description = String(node.files.length);
        item.tooltip = tooltip;
        item.iconPath = icon;
        item.contextValue = contextValue;
        return item;
      }
      case 'file': {
        const { file } = node;
        const item = new vscode.TreeItem(file.label);
        item.id = `project.${file.uri.toString()}`;
        item.tooltip = vscode.workspace.asRelativePath(file.uri);
        item.resourceUri = file.uri;
        item.contextValue = file.kind === 'script' ? ITEMS.projectScript(file.type) : ITEMS.projectSnippet;
        item.command = { title: 'Open', command: 'vscode.open', arguments: [file.uri] };
        return item;
      }
    }
  }

  private rootNode(root: vscode.Uri, files: ProjectFile[]): Node {
    const index = this.roots.findIndex((candidate) => candidate.toString() === root.toString());
    return { kind: 'root', root, label: rootLabels(this.roots)[index], files };
  }
}
