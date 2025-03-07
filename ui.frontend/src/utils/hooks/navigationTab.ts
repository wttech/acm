import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Key } from '@react-types/shared';

export function useNavigationTab(basePath: string, defaultTab: string = '') {
  const { tab } = useParams();
  const navigate = useNavigate();
  const [selectedTab, setSelectedTab] = useState<string>(tab || defaultTab);

  useEffect(() => {
    if (tab !== selectedTab) {
      navigate(`${basePath}/${selectedTab}`, { replace: true });
    }
  }, [selectedTab, tab, navigate, basePath]);

  const handleTabChange = (key: Key) => {
    setSelectedTab(key as string);
  };

  return [selectedTab, handleTabChange] as const;
}