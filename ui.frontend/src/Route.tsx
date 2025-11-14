import { Navigate } from 'react-router-dom';
import { useAppState } from './hooks/app';
import { State } from './types/main';

interface RouteProps {
  children: React.ReactNode;
  featureId?: keyof State['permissions']['features'];
}

export function Route({ children, featureId }: RouteProps) {
  const state = useAppState();

  if (featureId && !state.permissions.features[featureId]) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
}
