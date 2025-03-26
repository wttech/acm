import { Text } from '@adobe/react-spectrum';
import Draft from '@spectrum-icons/workflow/Draft';
import FileCode from '@spectrum-icons/workflow/FileCode';
import React from 'react';
import { Executable } from '../utils/api.types';
import { Strings } from '../utils/strings.ts';

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
    const scriptName = Strings.removeEnd(scriptPath, '.groovy');
    return (
      <>
        <FileCode size="XS" /> <Text>Script '{scriptName}'</Text>
      </>
    );
  } else {
    return <>{value.id}</>;
  }
};

export default ExecutableValue;
