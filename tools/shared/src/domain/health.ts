export interface HealthIssue {
  severity?: string;
  category?: string;
  issue: string;
  details?: string | null;
}

export interface HealthStatus {
  healthy: boolean;
  issues?: HealthIssue[];
}

const MAX_LISTED_ISSUES = 3;

/** Describes an instance that ACM's health checker found unhealthy; `undefined` when healthy or unknown. */
export function describeUnhealthy(health: HealthStatus | undefined): string | undefined {
  if (!health || health.healthy) {
    return undefined;
  }
  const issues = (health.issues ?? []).map((item) => item.issue).filter(Boolean);
  const listed = issues.slice(0, MAX_LISTED_ISSUES).join('; ');
  const more = issues.length > MAX_LISTED_ISSUES ? ` (+${issues.length - MAX_LISTED_ISSUES} more)` : '';
  return `Instance is unhealthy (detected by ACM)${listed ? `: ${listed}${more}` : ''}. Running scripts may be unsafe; automatic scripts wait until it recovers.`;
}

export interface HealthReport {
  level: 'ok' | 'warning';
  headline: string;
  summary: string;
  issues: HealthIssue[];
  more: number;
}

const severityRank = (issue: HealthIssue) => {
  const severity = (issue.severity ?? '').toLowerCase();
  return /critical|error/.test(severity) ? 0 : /warn/.test(severity) ? 1 : 2;
};

/** Structures the health of an instance for display: worst issues first, at most `limit` listed. */
export function describeHealth(health: HealthStatus | undefined, limit = MAX_LISTED_ISSUES): HealthReport | undefined {
  if (!health) {
    return undefined;
  }
  if (health.healthy) {
    return {
      level: 'ok',
      headline: 'Healthy',
      summary: 'Instance is stable. Scripts run normally and automatic scripts are active.',
      issues: [],
      more: 0,
    };
  }
  const all = (health.issues ?? [])
    .filter((item) => item.issue)
    .map((item, index) => ({ item, index }))
    .sort((a, b) => severityRank(a.item) - severityRank(b.item) || a.index - b.index)
    .map(({ item }) => item);
  return {
    level: 'warning',
    headline: 'Unstable',
    summary: 'Running scripts may be unsafe; automatic scripts wait until the instance recovers.',
    issues: all.slice(0, limit),
    more: Math.max(0, all.length - limit),
  };
}
