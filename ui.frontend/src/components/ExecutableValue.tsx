import React from 'react';
import { Text } from '@adobe/react-spectrum';
import Draft from '@spectrum-icons/workflow/Draft';
import FileCode from '@spectrum-icons/workflow/FileCode';
import { Executable } from '../utils/api.types';

interface ExecutableValueProps {
  value: Executable;
}

const ExecutableValue: React.FC<ExecutableValueProps> = ({ value: value }) => {
  if (value.id === 'console') {
    return (
      <>
        <Draft size="XS" /> <Text>Console</Text>
      </>
    );
  } else if (value.id.includes('/script/enabled/')) {
    const scriptPath = value.id.split('/script/enabled/')[1];
    return (
      <>
        <FileCode size="XS" /> <Text>Script '{scriptPath}'</Text>
      </>
    );
  } else {
    return <>{value.id}</>;
  }
};

export default ExecutableValue;
