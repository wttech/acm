import { useContext } from 'react';
import { AppContext } from '../AppContext';
import { State } from '../utils/api.types.ts';

export function useAppState(): State {
  const state = useContext(AppContext);
  if (!state) {
    throw new Error('Application state is not available!');
  }
  return state;
}
