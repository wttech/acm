import * as vscode from 'vscode';
import {
  PACKAGE_SCRIPT_ROOT,
  SCRIPT_EXTENSION,
  SNIPPET_EXTENSION,
  SNIPPET_INFO,
  SCRIPT_TYPE_INFO,
  enabledScriptTypes,
  scriptIdOf,
  scriptLabel,
  scriptTypeOf,
  stripExtension,
  type ScriptType,
} from '@acm/shared';
import { isMockEnabled } from '../mock';
import { IGNORED_GLOB } from './roots';

interface FileBase {
  uri: vscode.Uri;
  root: vscode.Uri;
  /** The folder the label is relative to. */
  folder: vscode.Uri;
  extension: string;
  /** Path under the folder, without the extension. */
  label: string;
}

export interface ProjectScript extends FileBase {
  kind: 'script';
  /** Repository ID of the script on an instance. */
  id: string;
  type: ScriptType;
}

export interface ProjectSnippet extends FileBase {
  kind: 'snippet';
}

export type ProjectFile = ProjectScript | ProjectSnippet;

/** A part of the project content: the scripts of one script type, or the snippets. */
export type Section = { kind: 'scriptType'; scriptType: ScriptType } | { kind: 'snippets' };

export function scriptFolder(root: vscode.Uri, scriptType: ScriptType): vscode.Uri {
  return vscode.Uri.joinPath(root, scriptType.toLowerCase());
}

/** Snippets live next to the scripts folder: `conf/acm/settings/snippet/available`. */
export function snippetsFolder(root: vscode.Uri): vscode.Uri {
  return vscode.Uri.joinPath(root, '..', 'snippet', 'available');
}

/** Every enabled script type, also when empty, so a script can be added to it; then the snippets. */
export function sections(): Section[] {
  const scriptTypes = enabledScriptTypes({ mock: isMockEnabled() }).map(
    (scriptType): Section => ({ kind: 'scriptType', scriptType }),
  );
  return [...scriptTypes, { kind: 'snippets' }];
}

export function sectionOf(file: ProjectFile): Section {
  return file.kind === 'script' ? { kind: 'scriptType', scriptType: file.type } : { kind: 'snippets' };
}

export function sectionKey(section: Section): string {
  return section.kind === 'scriptType' ? section.scriptType : 'SNIPPETS';
}

/** The name and the purpose of a section, as shown to the user. */
export function sectionInfo(section: Section): { label: string; description: string } {
  return section.kind === 'scriptType' ? SCRIPT_TYPE_INFO[section.scriptType] : SNIPPET_INFO;
}

export function filesIn(files: ProjectFile[], section: Section): ProjectFile[] {
  return files.filter((file) =>
    file.kind === 'script'
      ? section.kind === 'scriptType' && section.scriptType === file.type
      : section.kind === 'snippets',
  );
}

function find(folder: vscode.Uri, extension: string): Thenable<vscode.Uri[]> {
  return vscode.workspace.findFiles(new vscode.RelativePattern(folder, `**/*${extension}`), IGNORED_GLOB);
}

async function listScripts(root: vscode.Uri): Promise<ProjectScript[]> {
  const enabled = enabledScriptTypes({ mock: isMockEnabled() });
  return (await find(root, SCRIPT_EXTENSION)).flatMap((uri): ProjectScript[] => {
    const id = scriptIdOf(uri.path.slice(root.path.length));
    const type = scriptTypeOf(id);
    if (!type || !enabled.includes(type)) {
      return [];
    }
    return [
      { kind: 'script', uri, root, folder: scriptFolder(root, type), extension: SCRIPT_EXTENSION, label: scriptLabel(id), id, type },
    ];
  });
}

async function listSnippets(root: vscode.Uri): Promise<ProjectSnippet[]> {
  const folder = snippetsFolder(root);
  return (await find(folder, SNIPPET_EXTENSION)).map((uri): ProjectSnippet => ({
    kind: 'snippet',
    uri,
    root,
    folder,
    extension: SNIPPET_EXTENSION,
    label: stripExtension(uri.path.slice(folder.path.length + 1), SNIPPET_EXTENSION),
  }));
}

/** The scripts and snippets of a root, sorted by label. */
export async function listFiles(root: vscode.Uri): Promise<ProjectFile[]> {
  const [scripts, snippets] = await Promise.all([listScripts(root), listSnippets(root)]);
  return [...scripts, ...snippets].sort((a, b) => a.label.localeCompare(b.label));
}

/** Whether a changed file may alter the project content: a new root may appear or an existing one change. */
export function affectsProject(uri: vscode.Uri, roots: vscode.Uri[]): boolean {
  return (
    uri.path.includes(`${PACKAGE_SCRIPT_ROOT}/`) ||
    roots.some((root) => [root, snippetsFolder(root)].some((folder) => uri.path.startsWith(`${folder.path}/`)))
  );
}
