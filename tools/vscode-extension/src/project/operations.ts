import * as vscode from 'vscode';
import {
  SCRIPT_EXTENSION,
  SNIPPET_EXTENSION,
  snippetTemplate,
  stripExtension,
  templatesFor,
  validateRelativeName,
  type ScriptType,
} from '@acm/shared';
import { scriptFolder, sectionInfo, sections, snippetsFolder, type ProjectFile } from './files';

function fileExists(uri: vscode.Uri): Thenable<boolean> {
  return vscode.workspace.fs.stat(uri).then(
    () => true,
    () => false,
  );
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

async function writeAndOpen(file: vscode.Uri, content: string): Promise<void> {
  await vscode.workspace.fs.writeFile(file, new TextEncoder().encode(content));
  await vscode.window.showTextDocument(await vscode.workspace.openTextDocument(file));
}

/** Creates a script or a snippet, asking what kind first; returns the new file, `undefined` when cancelled. */
export async function createContent(root: vscode.Uri): Promise<vscode.Uri | undefined> {
  const picked = await vscode.window.showQuickPick(
    sections().map((section) => ({
      label: sectionInfo(section).label,
      detail: sectionInfo(section).description,
      section,
    })),
    { title: 'New ACM Content', placeHolder: 'Content type', matchOnDetail: true },
  );
  if (!picked) {
    return undefined;
  }
  return picked.section.kind === 'scriptType'
    ? createScript(root, picked.section.scriptType)
    : createSnippet(root);
}

/** Creates a script of the type from a template; returns the new file, `undefined` when cancelled. */
export async function createScript(root: vscode.Uri, type: ScriptType): Promise<vscode.Uri | undefined> {
  const template = await vscode.window.showQuickPick(
    templatesFor(type).map((candidate) => ({ label: candidate.name, detail: candidate.description, candidate })),
    { title: `New ${type.toLowerCase()} script`, placeHolder: 'Select ACM script template', matchOnDetail: true },
  );
  if (!template) {
    return undefined;
  }
  const name = await vscode.window.showInputBox({
    title: `New ${type.toLowerCase()} script`,
    prompt: 'Name or path under the type folder, e.g. example/ACME-1_hello',
    validateInput: (value) => validateRelativeName(value, SCRIPT_EXTENSION),
  });
  if (name === undefined) {
    return undefined;
  }
  const file = await freeFile(scriptFolder(root, type), SCRIPT_EXTENSION, name);
  await writeAndOpen(file, template.candidate.code);
  return file;
}

/** Creates a snippet from a template; returns the new file, `undefined` when cancelled. */
export async function createSnippet(root: vscode.Uri): Promise<vscode.Uri | undefined> {
  const name = await vscode.window.showInputBox({
    title: 'New snippet',
    prompt: 'Name or path under the snippets folder, e.g. acme/hello',
    validateInput: (value) => validateRelativeName(value, SNIPPET_EXTENSION),
  });
  if (name === undefined) {
    return undefined;
  }
  const file = await freeFile(snippetsFolder(root), SNIPPET_EXTENSION, name);
  await writeAndOpen(file, snippetTemplate(name));
  return file;
}

/** Renames a file; returns the new location, `undefined` when cancelled or unchanged. */
export async function renameFile(file: ProjectFile): Promise<vscode.Uri | undefined> {
  const name = await vscode.window.showInputBox({
    title: `Rename ${file.label}`,
    prompt: 'Name or path under the folder',
    value: file.label,
    validateInput: (value) => validateRelativeName(value, file.extension),
  });
  if (name === undefined || stripExtension(name.trim(), file.extension) === file.label) {
    return undefined;
  }
  const target = await freeFile(file.folder, file.extension, name);
  const edit = new vscode.WorkspaceEdit();
  edit.renameFile(file.uri, target);
  if (!(await vscode.workspace.applyEdit(edit))) {
    throw new Error(`Cannot rename ${vscode.workspace.asRelativePath(file.uri)}.`);
  }
  return target;
}

/** Copies a file and opens the copy; returns it, `undefined` when cancelled. */
export async function duplicateFile(file: ProjectFile): Promise<vscode.Uri | undefined> {
  const name = await vscode.window.showInputBox({
    title: `Duplicate ${file.label}`,
    prompt: 'Name or path of the copy under the folder',
    value: `${file.label}_copy`,
    validateInput: (value) => validateRelativeName(value, file.extension),
  });
  if (name === undefined) {
    return undefined;
  }
  const target = await freeFile(file.folder, file.extension, name);
  await vscode.workspace.fs.copy(file.uri, target);
  await vscode.window.showTextDocument(await vscode.workspace.openTextDocument(target));
  return target;
}

/** Moves a file to the trash after confirmation; tells whether it did. */
export async function deleteFile(file: ProjectFile): Promise<boolean> {
  const action = await vscode.window.showWarningMessage(
    `Delete ${file.label}?`,
    { modal: true, detail: 'The file is moved to the trash. A copy deployed to an instance is not affected.' },
    'Delete',
  );
  if (!action) {
    return false;
  }
  await vscode.workspace.fs.delete(file.uri, { useTrash: true });
  return true;
}
