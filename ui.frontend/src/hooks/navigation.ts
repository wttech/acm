import { Key } from '@react-types/shared';
import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

export function useNavigationTab(basePath: string | null, defaultTab: string = '') {
  const { tab } = useParams();
  const navigate = useNavigate();
  const [selectedTab, setSelectedTab] = useState<string>(tab || defaultTab);

  useEffect(() => {
    if (tab === undefined || tab === '') {
      setSelectedTab(defaultTab);
    } else {
      setSelectedTab(tab);
    }
  }, [tab, defaultTab]);

  useEffect(() => {
    if (basePath !== null && tab !== selectedTab) {
      navigate(`${basePath}/${selectedTab}`, { replace: true });
    }
  }, [selectedTab, navigate, basePath, tab]);

  const handleTabChange = (key: Key) => {
    setSelectedTab(key as string);
  };

  return [selectedTab, handleTabChange] as const;
}