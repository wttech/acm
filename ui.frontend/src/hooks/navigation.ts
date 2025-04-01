import { Key } from '@react-types/shared';
import { useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';

export enum NavigationSearchParams {
  TAB = 'tab',
}

export function useNavigationTab(defaultTab: string = '') {
  const [searchParams, setSearchParams] = useSearchParams();

  useEffect(() => {
    if (!searchParams.get(NavigationSearchParams.TAB)) {
      setSearchParams((prev) => {
        prev.set(NavigationSearchParams.TAB, defaultTab);
        return prev;
      });
    }
  }, [defaultTab, searchParams, setSearchParams]);

  const handleTabChange = (key: Key) => {
    setSearchParams((prev) => {
      prev.set(NavigationSearchParams.TAB, key.toString());
      return prev;
    });
  };

  return [searchParams.get(NavigationSearchParams.TAB) || defaultTab, handleTabChange] as const;
}
