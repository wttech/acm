import * as path from 'node:path';
import * as vscode from 'vscode';
import { ACM_API, AcmHttpError, SCRIPT_ROOT, scriptLabel } from '@acm/shared';
import { getTarget, type AcmInstance } from '../instances';
import { scriptUri } from '../views';
import type { ProjectScript } from './files';
import { IGNORED_GLOB } from './roots';

async function showDiff(instance: AcmInstance, id: string, local: vscode.Uri): Promise<void> {
  await vscode.commands.executeCommand(
    'vscode.diff',
    scriptUri(instance, id),
    local,
    `${scriptLabel(id)} (${instance.name}) \u2194 ${vscode.workspace.asRelativePath(local)}`,
  );
}

async function firstExisting(uris: vscode.Uri[]): Promise<vscode.Uri | undefined> {
  for (const uri of uris) {
    const exists = await vscode.workspace.fs.stat(uri).then(
      () => true,
      () => false,
    );
    if (exists) {
      return uri;
    }
  }
  return undefined;
}

/** Compares a script stored on the instance with its local file in one of the roots, or one the user picks. */
export async function compareInstanceScript(instance: AcmInstance, id: string, roots: vscode.Uri[]): Promise<void> {
  const relative = id.startsWith(SCRIPT_ROOT) ? id.slice(SCRIPT_ROOT.length) : undefined;
  let local =
    relative === undefined ? undefined : await firstExisting(roots.map((root) => vscode.Uri.joinPath(root, relative)));
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
    await showDiff(instance, id, local);
  }
}

/** Compares a project script with the version on the active instance; says so when it is not deployed there. */
export async function compareProjectScript(script: ProjectScript): Promise<void> {
  const target = await getTarget();
  if (!target) {
    return;
  }
  const deployed = await target.client
    .request<{ list?: unknown[] }>('GET', `${ACM_API.script}?id=${encodeURIComponent(script.id)}`)
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
    vscode.window.showInformationMessage(`ACM: ${script.label} is not on instance "${target.instance.name}" yet.`);
    return;
  }
  await showDiff(target.instance, script.id, script.uri);
}
