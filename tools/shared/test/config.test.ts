import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { CONFIG_KEYS, configKey, parseConfig } from '../src/api/config.ts';
import {
  defaultSecret,
  enabledInstances,
  resolveActive,
  resolveInstance,
  type AcmInstance,
} from '../src/domain/instance.ts';

const instance = (over: Partial<AcmInstance> = {}): AcmInstance => ({
  name: 'one',
  url: 'http://localhost:4502',
  authMode: 'basic',
  user: 'admin',
  ...over,
});

const doc = (instances: unknown[], extra: Record<string, unknown> = {}) => ({
  [configKey(CONFIG_KEYS.instances)]: instances,
  ...extra,
});

describe('parseConfig', () => {
  it('parses instances and applies timing defaults', () => {
    const config = parseConfig(doc([instance(), instance({ name: 'two', url: 'http://localhost:4503', readonly: true, enabled: false })]));
    assert.equal(config.instances.length, 2);
    assert.equal(config.httpTimeoutMs, 30000);
    assert.equal(config.runTimeoutMs, 120000);
    assert.equal(config.runPollIntervalMs, 1000);
    assert.equal(config.instances[1].enabled, false);
  });

  it('reads the active instance and timing keys', () => {
    const config = parseConfig(
      doc([instance()], {
        [configKey(CONFIG_KEYS.activeInstance)]: 'one',
        [configKey(CONFIG_KEYS.httpTimeout)]: 5000,
      }),
    );
    assert.equal(config.activeInstance, 'one');
    assert.equal(config.httpTimeoutMs, 5000);
  });

  it('accepts all secret forms', () => {
    const config = parseConfig(
      doc([
        instance({ secret: 'literal' }),
        instance({ name: 'two', url: 'http://x', authMode: 'bearer', secret: { env: 'T' } }),
        instance({ name: 'three', url: 'http://y', authMode: 'cookie', secret: { file: './c' } }),
      ]),
    );
    assert.equal(config.instances[0].secret, 'literal');
    assert.deepEqual(config.instances[1].secret, { env: 'T' });
    assert.deepEqual(config.instances[2].secret, { file: './c' });
  });

  it('rejects missing or malformed documents', () => {
    assert.throws(() => parseConfig(null), /must be an object/);
    assert.throws(() => parseConfig({}), /acm\.instances/);
    assert.throws(() => parseConfig(doc([])), /at least one/);
    assert.throws(() => parseConfig(doc([{ url: 'http://x', authMode: 'basic', user: 'a' }])), /"name"/);
    assert.throws(() => parseConfig(doc([instance(), instance()])), /duplicate/);
    assert.throws(() => parseConfig(doc([{ name: 'x', url: ':', authMode: 'basic', user: 'a' }])), /invalid "url"/);
    assert.throws(() => parseConfig(doc([{ name: 'x', url: 'http://x', authMode: 'oauth' }])), /authMode/);
    assert.throws(() => parseConfig(doc([{ name: 'x', url: 'http://x', authMode: 'basic' }])), /"user"/);
    assert.throws(() => parseConfig(doc([instance({ secret: 42 as never })] as AcmInstance[])), /"secret"/);
  });
});

describe('resolveInstance', () => {
  const two = [instance(), instance({ name: 'two', url: 'http://localhost:4503' })];

  it('returns the requested enabled instance', () => {
    assert.equal(resolveInstance(two, undefined, 'two').name, 'two');
  });

  it('falls back to the active one, then to the only enabled one', () => {
    assert.equal(resolveInstance(two, 'two').name, 'two');
    assert.equal(resolveInstance([instance()], undefined).name, 'one');
    assert.equal(resolveInstance([instance(), instance({ name: 'off', url: 'http://x', enabled: false })], undefined).name, 'one');
  });

  it('rejects disabled, unknown or ambiguous targets', () => {
    assert.throws(() => resolveInstance(two, undefined), /one, two/);
    assert.throws(() => resolveInstance(two, undefined, 'nope'), /"nope"/);
    assert.throws(() => resolveInstance([instance({ enabled: false })], undefined), /disabled/);
    assert.throws(() => resolveInstance(two, undefined, 'off'), /Enabled instances/);
  });
});

describe('resolveActive / enabledInstances', () => {
  it('never returns a disabled instance', () => {
    const all = [instance({ enabled: false }), instance({ name: 'two', url: 'http://x' })];
    assert.equal(enabledInstances(all).length, 1);
    assert.equal(resolveActive(all, 'one'), undefined);
    assert.equal(resolveActive(all, 'two')?.name, 'two');
  });
});

describe('defaultSecret', () => {
  it('is admin only for local basic auth with user admin', () => {
    assert.equal(defaultSecret(instance()), 'admin');
    assert.equal(defaultSecret(instance({ url: 'https://remote.example.com' })), undefined);
    assert.equal(defaultSecret(instance({ user: 'other' })), undefined);
    assert.equal(defaultSecret(instance({ authMode: 'bearer' })), undefined);
  });
});
