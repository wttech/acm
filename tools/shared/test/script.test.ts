import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { PACKAGE_SCRIPT_ROOT, enabledScriptTypes, scriptRootLabels, scriptRootsOf } from '../src/domain/script.ts';

const content = (module: string) => `/w/${module}/src/main/content${PACKAGE_SCRIPT_ROOT}`;

describe('enabledScriptTypes', () => {
  it('hides Mock while the mock feature is off', () => {
    assert.deepEqual(enabledScriptTypes({ mock: false }), ['MANUAL', 'AUTOMATIC', 'EXTENSION']);
  });

  it('lists every type when the mock feature is on', () => {
    assert.deepEqual(enabledScriptTypes({ mock: true }), ['MANUAL', 'AUTOMATIC', 'EXTENSION', 'MOCK']);
  });
});

describe('scriptRootLabels', () => {
  it('returns nothing for no roots', () => {
    assert.deepEqual(scriptRootLabels([]), []);
  });

  it('keeps the last segment of a single root', () => {
    assert.deepEqual(scriptRootLabels([content('ui.content')]), ['content']);
  });

  it('drops the shared prefix and suffix, keeping what tells the roots apart', () => {
    assert.deepEqual(scriptRootLabels([content('ui.content'), content('ui.content.example')]), [
      'ui.content',
      'ui.content.example',
    ]);
  });

  it('handles more than two roots and keeps their order', () => {
    assert.deepEqual(scriptRootLabels([content('ui.content.example'), content('ui.apps'), content('ui.content')]), [
      'ui.content.example',
      'ui.apps',
      'ui.content',
    ]);
  });

  it('keeps differing segments of different depths', () => {
    assert.deepEqual(scriptRootLabels(['/w/core/src/main/content', '/w/apps/x/src/main/content']), ['core', 'apps/x']);
  });

  it('keeps a differing segment in the middle together with the ones around it', () => {
    assert.deepEqual(scriptRootLabels(['/p/a/x/c', '/p/b/y/c']), ['a/x', 'b/y']);
  });

  it('keeps at least one segment when a root is the parent of another', () => {
    assert.deepEqual(scriptRootLabels(['/w/a', '/w/a/b']), ['a', 'a/b']);
  });

  it('works for roots that do not end with the package script root', () => {
    assert.deepEqual(scriptRootLabels(['/w/a', '/w/b']), ['a', 'b']);
  });

  it('normalizes Windows separators', () => {
    const windows = (module: string) => `C:\\w\\${module}\\src\\main\\content${PACKAGE_SCRIPT_ROOT.replace(/\//g, '\\')}`;
    assert.deepEqual(scriptRootLabels([windows('a'), windows('b')]), ['a', 'b']);
  });

  it('ignores trailing slashes', () => {
    assert.deepEqual(scriptRootLabels(['/w/a/', '/w/b/']), ['a', 'b']);
  });

  it('gives distinct, non-empty labels to distinct roots', () => {
    const sets = [
      [content('a'), content('b')],
      [content('ui.content'), content('ui.content.example'), content('ui.apps')],
      ['/w/a', '/w/a/b', '/w/a/b/c'],
      ['/p/a/x/c', '/p/b/y/c', '/p/a/y/c'],
      ['/w/a/src', '/w/b/src', '/x/a/src'],
      ['/a', '/b'],
    ];
    for (const roots of sets) {
      const labels = scriptRootLabels(roots);
      assert.equal(labels.length, roots.length);
      assert.ok(labels.every((label) => label !== ''), `empty label in ${JSON.stringify(labels)}`);
      assert.equal(new Set(labels).size, roots.length, `labels of ${JSON.stringify(roots)} are not distinct`);
    }
  });
});

describe('scriptRootsOf', () => {
  it('finds the distinct, sorted roots among file paths', () => {
    assert.deepEqual(
      scriptRootsOf([
        `${content('b')}/manual/x.groovy`,
        `${content('a')}/manual/y.groovy`,
        `${content('a')}/automatic/z.groovy`,
      ]),
      [content('a'), content('b')],
    );
  });

  it('ignores files outside a scripts root and the root folder itself', () => {
    assert.deepEqual(scriptRootsOf(['/w/a/src/Other.groovy', content('a')]), []);
  });

  it('normalizes Windows separators', () => {
    assert.deepEqual(scriptRootsOf([`C:\\w\\a${PACKAGE_SCRIPT_ROOT.replace(/\//g, '\\')}\\manual\\x.groovy`]), [
      `C:/w/a${PACKAGE_SCRIPT_ROOT}`,
    ]);
  });
});
