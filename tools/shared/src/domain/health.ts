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
