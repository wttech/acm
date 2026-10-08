import * as vscode from 'vscode';
import { SCRIPT_EXTENSION, SNIPPET_EXTENSION } from '@acm/shared';
import { registerCommand } from '../errors';
import { COMMANDS, SETTINGS, settingId } from '../ids';
import type { AcmInstance } from '../instances';
import { onDidChangeMock } from '../mock';
import { compareInstanceScript, compareProjectScript } from './compare';
import { affectsProject } from './files';
import { createContent, createScript, createSnippet, deleteFile, duplicateFile, renameFile } from './operations';
import { requireRoot } from './roots';
import { ProjectTree, fileOf, scriptOf, type Node } from './tree';

const WATCHED_FILES = `**/*{${SCRIPT_EXTENSION},${SNIPPET_EXTENSION}}`;

export function registerProjectContent(context: vscode.ExtensionContext): void {
  const tree = new ProjectTree();
  let timer: ReturnType<typeof setTimeout> | undefined;
  const refreshSoon = () => {
    clearTimeout(timer);
    timer = setTimeout(() => void tree.refresh(), 300);
  };
  const onFile = (uri: vscode.Uri) => {
    if (affectsProject(uri, tree.roots)) {
      refreshSoon();
    }
  };
  /** Shows the outcome of a change in the tree, selecting the file it produced. */
  const refreshAndReveal = async (uri?: vscode.Uri) => {
    await tree.refresh();
    if (uri) {
      await tree.reveal(uri);
    }
  };
  const watcher = vscode.workspace.createFileSystemWatcher(WATCHED_FILES, false, true, false);
  context.subscriptions.push(
    tree.view,
    watcher,
    watcher.onDidCreate(onFile),
    watcher.onDidDelete(onFile),
    { dispose: () => clearTimeout(timer) },
    vscode.workspace.onDidChangeWorkspaceFolders(refreshSoon),
    onDidChangeMock(refreshSoon),
    vscode.workspace.onDidChangeConfiguration((event) => {
      if (event.affectsConfiguration(settingId(SETTINGS.scriptsRoots))) {
        refreshSoon();
      }
    }),
    registerCommand(COMMANDS.refreshProjectContent, () => tree.refresh()),
    // From the view title or the palette VS Code passes the selected item, so the argument is ignored and the root is asked.
    registerCommand(COMMANDS.newProjectContent, async () => {
      const root = await requireRoot(tree.roots);
      if (root) {
        await refreshAndReveal(await createContent(root));
      }
    }),
    registerCommand(COMMANDS.newProjectScriptOfType, async (node?: Node) => {
      if (node?.kind === 'group' && node.section.kind === 'scriptType') {
        await refreshAndReveal(await createScript(node.root, node.section.scriptType));
      }
    }),
    registerCommand(COMMANDS.newProjectSnippet, async (node?: Node) => {
      if (node?.kind === 'group' && node.section.kind === 'snippets') {
        await refreshAndReveal(await createSnippet(node.root));
      }
    }),
    registerCommand(COMMANDS.runProjectScript, async (node?: Node) => {
      const script = scriptOf(node);
      if (script) {
        await vscode.window.showTextDocument(script.uri);
        await vscode.commands.executeCommand(COMMANDS.run);
      }
    }),
    registerCommand(COMMANDS.renameProjectFile, async (node?: Node) => {
      const file = fileOf(node);
      if (file) {
        await refreshAndReveal(await renameFile(file));
      }
    }),
    registerCommand(COMMANDS.duplicateProjectFile, async (node?: Node) => {
      const file = fileOf(node);
      if (file) {
        await refreshAndReveal(await duplicateFile(file));
      }
    }),
    registerCommand(COMMANDS.deleteProjectFile, async (node?: Node) => {
      const file = fileOf(node);
      if (file && (await deleteFile(file))) {
        await refreshAndReveal();
      }
    }),
    registerCommand(COMMANDS.revealProjectFile, async (node?: Node) => {
      const file = fileOf(node);
      if (file) {
        await vscode.commands.executeCommand('revealInExplorer', file.uri);
      }
    }),
    registerCommand(COMMANDS.compareProjectScript, async (node?: Node) => {
      const script = scriptOf(node);
      if (script) {
        await compareProjectScript(script);
      }
    }),
    registerCommand(COMMANDS.compareScript, async (node?: { instance?: AcmInstance; script?: { id: string } }) => {
      if (node?.instance && node.script) {
        await compareInstanceScript(node.instance, node.script.id, tree.roots);
      }
    }),
  );
  void tree.refresh();
}
