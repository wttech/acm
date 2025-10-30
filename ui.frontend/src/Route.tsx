
import { Navigate } from 'react-router-dom';
import { State } from './types/main';
import { useAppState } from './hooks/app';

interface RouteProps {
  children: React.ReactNode;
  permission?: keyof State['permissions'];
}

export function Route({ children, permission }: RouteProps) {
  const state = useAppState();

  if (permission && (!state.permissions[permission])) {
    return <Navigate to="/" replace/>;
  }

  return <>{children}</>;
}
