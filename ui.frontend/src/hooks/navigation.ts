import { Key } from '@react-types/shared';
import { useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';

export const TAB_SEARCH_PARAM_KEY = 'tab';

export function useNavigationTab(defaultTab: string = '') {
  const [searchParams, setSearchParams] = useSearchParams();

  useEffect(() => {
    if (!searchParams.get(TAB_SEARCH_PARAM_KEY)) {
      setSearchParams((prev) => {
        prev.set(TAB_SEARCH_PARAM_KEY, defaultTab);

        return prev;
      });
    }
  }, [defaultTab, searchParams, setSearchParams]);

  const handleTabChange = (key: Key) => {
    setSearchParams((prev) => {
      prev.set(TAB_SEARCH_PARAM_KEY, key.toString());

      return prev;
    });
  };

  return [searchParams.get(TAB_SEARCH_PARAM_KEY) || defaultTab, handleTabChange] as const;
}
