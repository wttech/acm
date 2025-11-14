import { useContext } from 'react';
import { AppContext } from '../AppContext';
import { FeatureId, State } from '../types/main.ts';

export function useAppState(): State {
  const state = useContext(AppContext);
  if (!state) {
    throw new Error('Application state is not available!');
  }
  return state;
}

export function useFeatureEnabled(id: FeatureId): boolean {
  const state = useAppState();
  return state.permissions.features[id] === true;
}
