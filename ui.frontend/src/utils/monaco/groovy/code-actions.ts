import { Monaco } from '@monaco-editor/react';
import { replaceWithShortClass } from './code-actions/replace-with-short-class.ts';

export function registerCodeActions(instance: Monaco) {
  replaceWithShortClass(instance);
}
