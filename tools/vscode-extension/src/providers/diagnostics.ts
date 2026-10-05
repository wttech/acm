import * as vscode from 'vscode';
import { ACM_API, CONSOLE_CODE_ID, normalizeGroovy, type AcmClient, type Execution } from '@acm/shared';
import { getActiveInstance, getClient } from '../instances';

const ACM_SCRIPT = /\bvoid\s+doRun\s*\(/;
const COMPILE_ERROR = /^(?:\S+: \d+: )?(.*?)\s*@ line (\d+), column (\d+)\.?\s*$/gm;
// normalizeGroovy() puts a bare snippet 5 lines down, indented by 4 spaces.
const WRAP_OFFSET = { line: 5, column: 4 };

let diagnostics: vscode.DiagnosticCollection;

export function registerDiagnostics(context: vscode.ExtensionContext): void {
  diagnostics = vscode.languages.createDiagnosticCollection('acm');
  context.subscriptions.push(
    diagnostics,
    vscode.workspace.onDidSaveTextDocument(async (document) => {
      const config = vscode.workspace.getConfiguration('acm');
      // Only ACM scripts: other Groovy files (e.g. build.gradle) are left alone.
      if (document.languageId !== 'groovy' || !config.get('validateOnSave') || !ACM_SCRIPT.test(document.getText())) {
        return;
      }
      const instance = getActiveInstance();
      const client = instance && (await getClient(instance, false));
      if (client) {
        await validateDocument(document, client).catch(() => undefined);
      }
    }),
    vscode.workspace.onDidCloseTextDocument((document) => diagnostics.delete(document.uri)),
  );
}

/** Compile-checks the document on the instance and publishes diagnostics; returns the compile error, if any. */
export async function validateDocument(document: vscode.TextDocument, client: AcmClient): Promise<string | undefined> {
  const text = document.getText();
  const content = normalizeGroovy(text);
  const offset = content === text ? { line: 0, column: 0 } : WRAP_OFFSET;
  const res = await client.request<Execution>('POST', ACM_API.executeCode, {
    mode: 'parse',
    code: { id: CONSOLE_CODE_ID, content },
  });
  const error = res.data?.error ?? undefined;
  diagnostics.set(document.uri, error ? toDiagnostics(document, error, offset) : []);
  return error;
}

function toDiagnostics(
  document: vscode.TextDocument,
  error: string,
  offset: { line: number; column: number },
): vscode.Diagnostic[] {
  const result: vscode.Diagnostic[] = [];
  for (const [, message, line, column] of error.matchAll(COMPILE_ERROR)) {
    const lineIndex = clamp(Number(line) - 1 - offset.line, document.lineCount - 1);
    const start = new vscode.Position(lineIndex, Math.max(0, Number(column) - 1 - offset.column));
    const range = document.getWordRangeAtPosition(start) ?? document.lineAt(lineIndex).range;
    result.push(toDiagnostic(range, message || error));
  }
  if (result.length === 0) {
    result.push(toDiagnostic(document.lineAt(0).range, error));
  }
  return result;
}

function toDiagnostic(range: vscode.Range, message: string): vscode.Diagnostic {
  const diagnostic = new vscode.Diagnostic(range, message, vscode.DiagnosticSeverity.Error);
  diagnostic.source = 'ACM';
  return diagnostic;
}

function clamp(line: number, max: number): number {
  return Math.min(Math.max(0, line), Math.max(0, max));
}
