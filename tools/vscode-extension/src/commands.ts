import * as os from 'node:os';
import * as path from 'node:path';
import * as vscode from 'vscode';
import {
  ACM_API,
  CONSOLE_CODE_ID,
  enabledTemplates,
  fetchConsoleOutput,
  isFailed,
  normalizeGroovy,
  waitForExecution,
  type Execution,
  type ExecutionListOutput,
  type QueueOutput,
} from '@acm/shared';
import { registerCommand as register, showError } from './errors';
import { COMMANDS, DISPLAY_NAME, SETTINGS, settingId } from './ids';
import { confirmInputs, promptInputs } from './inputs';
import {
  confirmRun,
  getActiveInstance,
  getClient,
  getSettings,
  getTarget,
  pickInstance,
  setActiveInstance,
  setCredentials,
  type AcmInstance,
  type AcmTarget,
} from './instances';
import { isMockEnabled } from './mock';
import { validateDocument } from './providers/diagnostics';
import { refreshHealth, setRunning } from './status';
import { executionUri, storedScript, type Views } from './views';

interface ExecutionNode {
  instance: AcmInstance;
  execution: Execution;
}

interface OutputInfo {
  name: string;
  type?: string;
  label?: string;
  downloadName?: string;
}

let output: vscode.OutputChannel;
let running: (AcmTarget & { executionId: string }) | undefined;

export function registerCommands(context: vscode.ExtensionContext, views: Views): void {
  output = vscode.window.createOutputChannel(DISPLAY_NAME);
  context.subscriptions.push(
    output,
    register(COMMANDS.run, () => run(false, views)),
    register(COMMANDS.runScript, (node?: { instance?: AcmInstance; script?: { id: string } }) =>
      runStored(node?.script && node.instance && { id: node.script.id, instance: node.instance.name }, views),
    ),
    register(COMMANDS.runSelection, () => run(true, views)),
    register(COMMANDS.runWithoutHistory, () => run(true, views, false)),
    register(COMMANDS.runWithInputs, confirmInputs),
    register(COMMANDS.validate, validate),
    register(COMMANDS.describe, describe),
    register(COMMANDS.abort, (node?: ExecutionNode) => abort(views, node)),
    register(COMMANDS.downloadOutputs, (node?: ExecutionNode) => downloadOutputs(node)),
    register(COMMANDS.selectInstance, selectInstance),
    register(COMMANDS.setCredentials, setCredentialsCommand),
    register(COMMANDS.checkConnection, checkConnection),
    register(COMMANDS.newScript, newScript),
  );
}

async function newScript(): Promise<void> {
  const picked = await vscode.window.showQuickPick(
    enabledTemplates({ mock: isMockEnabled() }).map((template) => ({
      label: template.name,
      description: template.target.toLowerCase(),
      detail: template.description,
      template,
    })),
    { placeHolder: 'Select ACM script template', matchOnDetail: true },
  );
  if (picked) {
    const document = await vscode.workspace.openTextDocument({ language: 'groovy', content: picked.template.code });
    await vscode.window.showTextDocument(document);
  }
}

async function runStored(script: StoredScript | undefined, views: Views): Promise<void> {
  if (script) {
    await run(false, views, true, script);
  }
}

interface StoredScript {
  id: string;
  instance: string;
}

interface RunSource {
  code: { id: string; content?: string };
  label: string;
  selection: boolean;
  hasInputs: boolean;
  /** Instance the stored script was read from; it must be the one that runs it. */
  instance?: string;
}

function storedSource(script: StoredScript): RunSource {
  return {
    code: { id: script.id },
    label: path.posix.basename(script.id),
    selection: false,
    hasInputs: true,
    instance: script.instance,
  };
}

/** Stored scripts run by ID so their executions are recorded under the script; other editors run as console code. */
function runSource(selectionOnly: boolean, script?: StoredScript): RunSource | undefined {
  if (script) {
    return storedSource(script);
  }
  const editor = vscode.window.activeTextEditor;
  if (!editor) {
    return undefined;
  }
  const selection = selectionOnly && !editor.selection.isEmpty ? editor.selection : undefined;
  const stored = selection ? undefined : storedScript(editor.document.uri);
  if (stored) {
    return storedSource(stored);
  }
  const content = normalizeGroovy(editor.document.getText(selection));
  return {
    code: { id: CONSOLE_CODE_ID, content },
    label: path.basename(editor.document.fileName),
    selection: !!selection,
    hasInputs: /\bdescribeRun\s*\(/.test(content),
  };
}

async function run(selectionOnly: boolean, views: Views, history = true, script?: StoredScript): Promise<void> {
  const source = runSource(selectionOnly, script);
  if (!source) {
    vscode.window.showWarningMessage('ACM: Open a Groovy script to run.');
    return;
  }
  if (running) {
    vscode.window.showWarningMessage(`ACM: Execution ${running.executionId} is still running.`);
    return;
  }
  const target = await getTarget();
  if (!target) {
    return;
  }
  if (source.instance && source.instance !== target.instance.name) {
    throw new Error(
      `${source.label} was opened from instance "${source.instance}", but "${target.instance.name}" is active. Switch back or reopen the script.`,
    );
  }
  if (!(await confirmRun(target.instance, 'Run script'))) {
    return;
  }
  const { instance, client } = target;
  const { code, label } = source;
  const inputs = source.hasInputs ? await promptInputs(client, code) : {};
  if (!inputs) {
    return;
  }

  output.show(true);
  output.appendLine(
    `Running ${label}${source.selection ? ' (selection)' : ''} on ${instance.name} (${instance.url})${history ? '' : ' without history'}`,
  );
  const inputValues = Object.keys(inputs).length > 0 ? inputs : undefined;
  if (!history) {
    const timeout = getSettings().runTimeout;
    // A run without history is one synchronous request, so it is bounded by ${settingId(SETTINGS.runTimeout)}.
    const res = await vscode.window
      .withProgress(
        { location: vscode.ProgressLocation.Notification, title: `ACM: Script ${label} on ${instance.name} (no history)` },
        () =>
          client.request<Execution>(
            'POST',
            ACM_API.executeCode,
            { mode: 'RUN', history: false, code, inputs: inputValues },
            true,
            timeout,
          ),
      )
      .then(undefined, (e: unknown) => {
        if (e instanceof Error && e.name === 'AbortError') {
          throw new Error(
            `No response within ${timeout / 1000} s (${settingId(SETTINGS.runTimeout)}). A run without history cannot be followed or aborted and may still be running on AEM; run longer scripts with history.`,
          );
        }
        throw e;
      });
    append(res.data.output, 0);
    report(label, res.data, instance, false);
    return;
  }
  const res = await client.request<QueueOutput>('POST', ACM_API.queueCode, {
    code,
    inputs: inputValues,
  });
  const queued = res.data?.executions?.[0];
  if (!queued) {
    throw new Error(`Queued, but no execution returned: ${res.message}`);
  }
  let execution: Execution = queued;
  const executionId = execution.id;

  running = { ...target, executionId };
  setRunning(executionId);
  views.refreshExecutions();
  try {
    let printed = 0;
    await vscode.window.withProgress(
      { location: vscode.ProgressLocation.Notification, title: `ACM: Script ${label} on ${instance.name}`, cancellable: true },
      async (progress, token) => {
        token.onCancellationRequested(() => abort(views).catch(showError));
        execution = await waitForExecution(client, execution, {
          intervalMs: getSettings().runPollInterval,
          onPoll: (current) => {
            progress.report({ message: current.status.toLowerCase() });
            printed = append(current.output, printed);
          },
        });
      },
    );
    append((await fetchConsoleOutput(client, executionId)) ?? execution.output, printed);
    report(label, execution, instance);
  } finally {
    running = undefined;
    setRunning(undefined);
    views.refreshExecutions();
  }
}

function append(text: string | null | undefined, printed: number): number {
  if (!text || text.length <= printed) {
    return printed;
  }
  output.append(text.slice(printed));
  return text.length;
}

function report(label: string, execution: Execution, instance: AcmInstance, recorded = true): void {
  if (execution.error) {
    output.appendLine(`\n${execution.error}`);
  }
  const status = execution.status.toUpperCase();
  output.appendLine(
    `\n${status}${execution.duration !== undefined ? ` in ${execution.duration} ms` : ''}${recorded ? ` (execution ${execution.id})` : ' (not recorded in history)'}\n`,
  );
  const open = () => vscode.commands.executeCommand('vscode.open', executionUri(instance, execution.id));
  if (isFailed(status)) {
    const actions = recorded ? ['Show Output', 'Open Execution'] : ['Show Output'];
    vscode.window.showErrorMessage(`ACM: ${label} ${status}.`, ...actions).then((action) => {
      if (action === 'Show Output') output.show();
      if (action === 'Open Execution') open();
    });
  } else if (status === 'SKIPPED') {
    vscode.window.showInformationMessage(`ACM: ${label} SKIPPED, canRun() returned false.`);
  } else {
    vscode.window.setStatusBarMessage(`$(pass) ACM: ${label} ${status}`, 5000);
  }
}

async function downloadOutputs(node?: ExecutionNode): Promise<void> {
  if (!node) {
    return;
  }
  const { instance, execution } = node;
  const client = await getClient(instance);
  if (!client) {
    return;
  }
  const id = encodeURIComponent(execution.id);
  const res = await client.request<ExecutionListOutput>('GET', `${ACM_API.execution}?id=${id}&format=full`);
  const outputs = (res.data?.list?.[0]?.outputs ?? []) as OutputInfo[];
  const picked = await vscode.window.showQuickPick(
    [
      ...outputs.map((o) => {
        const fileName = o.type === 'TEXT' ? `${o.name}.md` : o.downloadName || o.name;
        return { label: o.label || o.name, description: fileName, name: o.name, fileName };
      }),
      { label: 'Console', description: 'console.log', name: 'console', fileName: 'console.log' },
      {
        label: 'All outputs',
        description: 'ZIP archive with the console and all outputs',
        name: 'archive',
        fileName: `execution-${execution.id.replace(/\//g, '-')}.outputs.zip`,
      },
    ],
    { placeHolder: 'Select output to download' },
  );
  if (!picked) {
    return;
  }
  const folder = vscode.workspace.workspaceFolders?.[0]?.uri ?? vscode.Uri.file(os.homedir());
  const target = await vscode.window.showSaveDialog({ defaultUri: vscode.Uri.joinPath(folder, picked.fileName) });
  if (!target) {
    return;
  }
  const file = await client.requestBytes(
    `${ACM_API.executionOutput}?executionId=${id}&name=${encodeURIComponent(picked.name)}`,
  );
  if (file.status !== 200) {
    throw new Error(`Output '${picked.label}' is not available (HTTP ${file.status}).`);
  }
  await vscode.workspace.fs.writeFile(target, file.bytes);
  const action = await vscode.window.showInformationMessage(`ACM: Saved ${path.basename(target.fsPath)}.`, 'Open');
  if (action) {
    await vscode.commands.executeCommand('vscode.open', target);
  }
}

async function validate(): Promise<void> {
  const editor = vscode.window.activeTextEditor;
  const target = editor && (await getTarget());
  if (!editor || !target) {
    return;
  }
  const error = await validateDocument(editor.document, target.client);
  if (error) {
    vscode.window.showErrorMessage('ACM: Script does not compile, see Problems.');
  } else {
    vscode.window.showInformationMessage(`ACM: Script compiles on ${target.instance.name}.`);
  }
}

async function describe(): Promise<void> {
  const editor = vscode.window.activeTextEditor;
  const target = editor && (await getTarget());
  // ACM runs describeRun() to resolve inputs, so it is guarded like a run.
  if (!editor || !target || !(await confirmRun(target.instance, 'Describe inputs'))) {
    return;
  }
  const res = await target.client.request<{ inputs?: unknown }>('POST', ACM_API.describeCode, {
    code: { id: CONSOLE_CODE_ID, content: normalizeGroovy(editor.document.getText()) },
  });
  output.appendLine(`Inputs of ${path.basename(editor.document.fileName)} on ${target.instance.name}:`);
  output.appendLine(JSON.stringify(res.data?.inputs ?? {}, null, 2));
  output.show(true);
}

async function abort(views: Views, node?: { instance: AcmInstance; execution: Execution }): Promise<void> {
  const instance = node?.instance ?? running?.instance;
  const executionId = node?.execution.id ?? running?.executionId;
  if (!instance || !executionId) {
    vscode.window.showInformationMessage('ACM: No execution is running.');
    return;
  }
  if (instance.readonly) {
    vscode.window.showErrorMessage(`ACM: Instance "${instance.name}" is read-only, abort is blocked.`);
    return;
  }
  const client = node ? await getClient(instance) : running?.client;
  if (!client) {
    return;
  }
  await client.request('DELETE', `${ACM_API.queueCode}?executionId=${encodeURIComponent(executionId)}`);
  vscode.window.setStatusBarMessage(`ACM: Abort requested for ${executionId}`, 5000);
  views.refreshExecutions();
}

async function selectInstance(): Promise<void> {
  const instance = await pickInstance();
  if (instance) {
    await setActiveInstance(instance.name);
  }
}

async function setCredentialsCommand(): Promise<void> {
  const instance = getActiveInstance() ?? (await pickInstance('Select instance to set credentials for'));
  if (instance && (await setCredentials(instance))) {
    vscode.window.showInformationMessage(`ACM: Credentials for "${instance.name}" saved.`);
  }
}

async function checkConnection(): Promise<void> {
  const target = await getTarget();
  if (!target) {
    return;
  }
  await target.client.request('GET', ACM_API.state);
  void refreshHealth();
  vscode.window.showInformationMessage(
    `ACM: Connected to "${target.instance.name}" (${target.instance.url}) using ${target.client.authDescription}.`,
  );
}
