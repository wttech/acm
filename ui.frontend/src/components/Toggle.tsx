import { ReactNode } from 'react';

export interface ToggleProps {
  when: boolean;
  fallback?: ReactNode;
  children?: ReactNode;
}

export const Toggle = ({ when, fallback = null, children }: ToggleProps) => {
  return when ? <>{children}</> : <>{fallback}</>;
};

export default Toggle;
