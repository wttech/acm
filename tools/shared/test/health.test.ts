import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { describeHealth } from '../src/domain/health.ts';

describe('describeHealth', () => {
  it('is undefined when health is unknown', () => {
    assert.equal(describeHealth(undefined), undefined);
  });

  it('reports a healthy instance without issues', () => {
    const report = describeHealth({ healthy: true });
    assert.equal(report?.level, 'ok');
    assert.deepEqual(report?.issues, []);
  });

  it('lists the worst issues first and counts the rest', () => {
    const report = describeHealth(
      {
        healthy: false,
        issues: [
          { severity: 'WARNING', issue: 'b' },
          { severity: 'CRITICAL', issue: 'a' },
          { severity: 'INFO', issue: 'c' },
          { issue: '' },
        ],
      },
      2,
    );
    assert.equal(report?.level, 'warning');
    assert.deepEqual(report?.issues.map((item) => item.issue), ['a', 'b']);
    assert.equal(report?.more, 1);
  });
});
