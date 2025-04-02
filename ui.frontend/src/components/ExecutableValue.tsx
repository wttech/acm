import { Text } from '@adobe/react-spectrum';
import Draft from '@spectrum-icons/workflow/Draft';
import Hand from '@spectrum-icons/workflow/Hand';
import FlashOn from '@spectrum-icons/workflow/FlashOn';
import React from 'react';
import {Executable, isExecutableConsole} from '../utils/api.types';
import { Strings } from '../utils/strings.ts';

interface ExecutableValueProps {
  value: Executable;
}

const ExecutableValue: React.FC<ExecutableValueProps> = ({ value: value }) => {
  if (isExecutableConsole(value.id)) {
    return (
      <>
        <Draft size="XS" /> <Text>Console</Text>
      </>
    );
  } else if (value.id.includes('/script/auto/enabled/')) {
    const scriptPath = value.id.split('/script/auto/enabled/')[1];
    const scriptName = Strings.removeEnd(scriptPath, '.groovy');
    return (
      <>
        <FlashOn size="XS" /> <Text>Script '{scriptName}'</Text>
      </>
    );
  } else if (value.id.includes('/script/manual/')) {
    const scriptPath = value.id.split('/script/manual/')[1];
    const scriptName = Strings.removeEnd(scriptPath, '.groovy');
    return (
        <>
          <Hand size="XS" /> <Text>Script '{scriptName}'</Text>
        </>
    );
  } else {
    return <>{value.id}</>;
  }
};

export default ExecutableValue;
