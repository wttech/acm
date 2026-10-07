import * as path from 'node:path';
import * as vscode from 'vscode';
import { PACKAGE_SCRIPT_ROOT, scriptRootLabels, scriptRootsOf } from '@acm/shared';
import { SETTINGS, settingId } from '../ids';
import { readSetting } from '../instances';

export const IGNORED_GLOB = '**/{node_modules,target,.git}/**';

const ROOT_GLOB = `**${PACKAGE_SCRIPT_ROOT}/**`;

function workspaceFolder(): vscode.WorkspaceFolder | undefined {
  return vscode.workspace.workspaceFolders?.[0];
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

async function discoverRoots(): Promise<vscode.Uri[]> {
  const folder = workspaceFolder();
  if (!folder) {
    return [];
  }
  const files = await vscode.workspace.findFiles(ROOT_GLOB, IGNORED_GLOB, 5000);
  return scriptRootsOf(files.map((file) => file.path)).map((root) => folder.uri.with({ path: root }));
}

/** The scripts folders of the project: the configured ones, or else the ones found in the workspace. */
export async function resolveRoots(): Promise<vscode.Uri[]> {
  const configured = configuredRoots();
  return configured.length > 0 ? configured : discoverRoots();
}

/** Where a root is, relative to the workspace and without the scripts folder of the content package. */
export function rootDescription(root: vscode.Uri): string {
  return vscode.workspace.asRelativePath(root).replace(new RegExp(`${PACKAGE_SCRIPT_ROOT}$`), '');
}

export function rootLabels(roots: vscode.Uri[]): string[] {
  return scriptRootLabels(roots.map((root) => root.path));
}

/** The only root, else the one the user picks; `undefined` when there is none or the pick is cancelled. */
export async function requireRoot(roots: vscode.Uri[]): Promise<vscode.Uri | undefined> {
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
  const labels = rootLabels(roots);
  const picked = await vscode.window.showQuickPick(
    roots.map((root, index) => ({ label: labels[index], description: rootDescription(root), root })),
    { title: 'ACM Scripts Folder', placeHolder: "Select the project's folder with ACM scripts" },
  );
  return picked?.root;
}
