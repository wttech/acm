import { Link, StatusLight, Text } from '@adobe/react-spectrum';
import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAppState } from '../hooks/app';

const ExecutorStatusLight: React.FC = () => {
  const appState = useAppState();
  const navigate = useNavigate();

  return (
    <StatusLight id="executor-status" variant={appState.healthStatus.healthy ? 'positive' : 'negative'}>
      {appState.healthStatus.healthy ? (
        <Text>Executor active</Text>
      ) : (
        <>
          <Text>Executor paused</Text>
          <Text>&nbsp;&mdash;&nbsp;</Text>
          <Link isQuiet onPress={() => navigate('/maintenance?tab=health-checker')}>
            See health issues
          </Link>
        </>
      )}
    </StatusLight>
  );
};

export default ExecutorStatusLight;
