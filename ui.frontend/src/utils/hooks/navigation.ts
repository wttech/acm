import {useEffect, useRef, useState} from 'react';
import {useBlocker, useNavigate, useParams} from 'react-router-dom';
import { Key } from '@react-types/shared';
import {ToastQueue} from "@react-spectrum/toast";

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

// TODO https://github.com/wunderman-thompson/wtpl-aem-content-manager/issues/87
export function useNavigationPrevention(isExecuting: boolean, message: string){
  const isExecutingRef = useRef(isExecuting);

  useEffect(() => {
    isExecutingRef.current = isExecuting;
  }, [isExecuting]);

  useEffect(() => {
    const handleBeforeUnload = (e: BeforeUnloadEvent) => {
      if (isExecutingRef.current) {
        e.preventDefault();
        e.returnValue = '';
      }
    };
    window.addEventListener('beforeunload', handleBeforeUnload);
    return () => window.removeEventListener('beforeunload', handleBeforeUnload);
  }, []);

  useBlocker(() => {
    if (isExecutingRef.current) {
      ToastQueue.info(message, { timeout: 5000 });
      return true;
    }
    return false;
  });
}