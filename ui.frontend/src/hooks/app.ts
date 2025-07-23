import { computed, signal } from '@preact/signals-react';
import { InstanceRole, InstanceType, State } from '../utils/api.types';

export const appState = signal<State>({
  spaSettings: {
    appStateInterval: 3000,
    executionPollInterval: 1400,
    scriptStatsLimit: 30,
  },
  healthStatus: {
    healthy: true,
    issues: [],
  },
  mockStatus: {
    enabled: true,
  },
  instanceSettings: {
    id: 'default',
    timezoneId: 'UTC',
    role: InstanceRole.AUTHOR,
    type: InstanceType.CLOUD_CONTAINER,
  },
});

export const healthIssues = computed(() => appState.value.healthStatus.issues);
