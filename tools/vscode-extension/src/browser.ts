import * as vscode from 'vscode';
import {
  ACM_UI,
  acmUiExecutionRoute,
  acmUiScriptRoute,
  acmUiUrl,
  type AcmUiPage,
  type Execution,
} from '@acm/shared';
import { registerCommand } from './errors';
import { COMMANDS } from './ids';
import { getActiveInstance, pickInstance, type AcmInstance } from './instances';

interface InstanceNode {
  instance?: AcmInstance;
}

/** A command link for markdown that opens the given page of the ACM UI. */
export function openUiLink(page: AcmUiPage): string {
  return `command:${COMMANDS.openUi}?${encodeURIComponent(JSON.stringify([page]))}`;
}

async function open(instance: AcmInstance, route?: string): Promise<void> {
  const base = vscode.Uri.parse(acmUiUrl(instance.url));
  // Parsing a URL with the route would decode its %2F, and the ACM UI router needs them to keep an ID in one segment.
  await vscode.env.openExternal(route ? base.with({ fragment: route }) : base);
}

/** Opens a page of the ACM web UI on the active instance; asks which page when none is given. */
async function openPage(page?: AcmUiPage): Promise<void> {
  const instance = getActiveInstance() ?? (await pickInstance());
  if (!instance) {
    return;
  }
  const picked =
    page ??
    (
      await vscode.window.showQuickPick(
        (Object.keys(ACM_UI) as AcmUiPage[]).map((key) => ({ label: key.charAt(0).toUpperCase() + key.slice(1), key })),
        { title: `Open ACM on ${instance.name}`, placeHolder: 'Page' },
      )
    )?.key;
  if (picked) {
    await open(instance, ACM_UI[picked]);
  }
}

export function registerBrowser(context: vscode.ExtensionContext): void {
  context.subscriptions.push(
    registerCommand(COMMANDS.openUi, openPage),
    registerCommand(COMMANDS.openScriptInUi, async (node?: InstanceNode & { script?: { id: string } }) => {
      if (node?.instance && node.script) {
        await open(node.instance, acmUiScriptRoute(node.script.id));
      }
    }),
    registerCommand(COMMANDS.openExecutionInUi, async (node?: InstanceNode & { execution?: Execution }) => {
      if (node?.instance && node.execution) {
        await open(node.instance, acmUiExecutionRoute(node.execution.id));
      }
    }),
  );
}
