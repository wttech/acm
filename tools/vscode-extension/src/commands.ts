import * as os from 'node:os';
import * as path from 'node:path';
import * as vscode from 'vscode';
import {
  ACM_API,
  AcmHttpError,
  CONSOLE_CODE_ID,
  fetchConsoleOutput,
  fetchExecutionById,
  isFailed,
  isPending,
  normalizeGroovy,
  SCRIPT_TEMPLATES,
  type Execution,
  type ExecutionListOutput,
  type QueueOutput,
} from '@acm/shared';
import { promptInputs } from './inputs';
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
import { validateDocument } from './providers/diagnostics';
import { refreshHealth, setRunning } from './status';
import { executionUri, type Views } from './views';

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
  output = vscode.window.createOutputChannel('ACM');
  const register = (command: string, handler: (...args: never[]) => Promise<unknown>) =>
    vscode.commands.registerCommand(command, (...args: unknown[]) => handler(...(args as never[])).catch(showError));
  context.subscriptions.push(
    output,
    register('acm.run', () => run(false, views)),
    register('acm.runSelection', () => run(true, views)),
    register('acm.runWithoutHistory', () => run(true, views, false)),
    register('acm.validate', validate),
    register('acm.describe', describe),
    register('acm.abort', (node?: ExecutionNode) => abort(views, node)),
    register('acm.downloadOutputs', (node?: ExecutionNode) => downloadOutputs(node)),
    register('acm.selectInstance', selectInstance),
    register('acm.setCredentials', setCredentialsCommand),
    register('acm.checkConnection', checkConnection),
    register('acm.newScript', newScript),
  );
}

async function newScript(): Promise<void> {
  const picked = await vscode.window.showQuickPick(
    SCRIPT_TEMPLATES.map((template) => ({ label: template.name, detail: template.description, template })),
    { placeHolder: 'Select ACM script template', matchOnDetail: true },
  );
  if (picked) {
    const document = await vscode.workspace.openTextDocument({ language: 'groovy', content: picked.template.code });
    await vscode.window.showTextDocument(document);
  }
}

async function run(selectionOnly: boolean, views: Views, history = true): Promise<void> {
  const editor = vscode.window.activeTextEditor;
  if (!editor) {
    vscode.window.showWarningMessage('ACM: Open a Groovy script to run.');
    return;
  }
  if (running) {
    vscode.window.showWarningMessage(`ACM: Execution ${running.executionId} is still running.`);
    return;
  }
  const target = await getTarget();
  if (!target || !(await confirmRun(target.instance, 'Run script'))) {
    return;
  }
  const { instance, client } = target;
  const selection = selectionOnly && !editor.selection.isEmpty ? editor.selection : undefined;
  const content = normalizeGroovy(editor.document.getText(selection));
  const inputs = /\bdescribeRun\s*\(/.test(content) ? await promptInputs(client, content) : {};
  if (!inputs) {
    return;
  }
  const label = path.basename(editor.document.fileName);

  output.show(true);
  output.appendLine(
    `Running ${label}${selection ? ' (selection)' : ''} on ${instance.name} (${instance.url})${history ? '' : ' without history'}`,
  );
  const inputValues = Object.keys(inputs).length > 0 ? inputs : undefined;
  if (!history) {
    const timeout = getSettings().runTimeout;
    // A run without history is one synchronous request, so it is bounded by acm.run.timeout.
    const res = await vscode.window
      .withProgress(
        { location: vscode.ProgressLocation.Notification, title: `ACM: Script ${label} on ${instance.name} (no history)` },
        () =>
          client.request<Execution>(
            'POST',
            ACM_API.executeCode,
            { mode: 'RUN', history: false, code: { id: CONSOLE_CODE_ID, content }, inputs: inputValues },
            true,
            timeout,
          ),
      )
      .then(undefined, (e: unknown) => {
        if (e instanceof Error && e.name === 'AbortError') {
          throw new Error(
            `No response within ${timeout / 1000} s (acm.run.timeout). A run without history cannot be followed or aborted and may still be running on AEM; run longer scripts with history.`,
          );
        }
        throw e;
      });
    append(res.data.output, 0);
    report(label, res.data, instance, false);
    return;
  }
  const res = await client.request<QueueOutput>('POST', ACM_API.queueCode, {
    code: { id: CONSOLE_CODE_ID, content },
    inputs: inputValues,
  });
  let execution = res.data?.executions?.[0];
  if (!execution) {
    throw new Error(`Queued, but no execution returned: ${res.message}`);
  }
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
        while (isPending(execution!.status)) {
          progress.report({ message: execution!.status.toLowerCase() });
          await new Promise((resolve) => setTimeout(resolve, getSettings().runPollInterval));
          execution = (await fetchExecutionById(client, executionId)) ?? execution;
          printed = append(execution!.output, printed);
        }
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

function showError(error: unknown): void {
  const message = `ACM: ${error instanceof Error ? error.message : String(error)}`;
  if (error instanceof AcmHttpError && error.httpStatus === 401) {
    vscode.window.showErrorMessage(message, 'Set Credentials').then((action) => {
      if (action) vscode.commands.executeCommand('acm.setCredentials');
    });
  } else {
    vscode.window.showErrorMessage(message);
  }
}
