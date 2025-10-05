
export type HealthStatus = {
  healthy: boolean;
  issues: HealthIssue[];
};

export type HealthIssue = {
  category: HealthIssueCategory;
  issue: string;
  details: string;
  severity: HealthIssueSeverity;
};

export enum HealthIssueCategory {
  INSTANCE = 'INSTANCE',
  REPOSITORY = 'REPOSITORY',
  OSGI = 'OSGI',
  INSTALLER = 'INSTALLER',
  CODE_EXECUTOR = 'CODE_EXECUTOR',
  OTHER = 'OTHER'
}

export enum HealthIssueSeverity {
  CRITICAL = 'CRITICAL',
  WARNING = 'WARNING',
  INFO = 'INFO'
}
