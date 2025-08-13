import { Text } from '@adobe/react-spectrum';
import Draft from '@spectrum-icons/workflow/Draft';
import FlashOn from '@spectrum-icons/workflow/FlashOn';
import Hand from '@spectrum-icons/workflow/Hand';
import React from 'react';
import { isExecutableConsole } from '../utils/api.types';
import { Strings } from '../utils/strings.ts';

interface ExecutableIdValueProps {
  id: string;
}

const ExecutableIdValue: React.FC<ExecutableIdValueProps> = ({ id }) => {
  if (isExecutableConsole(id)) {
    return (
      <>
        <Draft size="XS" /> <Text>Console</Text>
      </>
    );
  } else if (id.includes('/script/automatic/')) {
    const scriptPath = id.split('/script/automatic/')[1];
    const scriptName = Strings.removeEnd(scriptPath, '.groovy');
    return (
      <>
        <FlashOn size="XS" /> <Text>Script '{scriptName}'</Text>
      </>
    );
  } else if (id.includes('/script/manual/')) {
    const scriptPath = id.split('/script/manual/')[1];
    const scriptName = Strings.removeEnd(scriptPath, '.groovy');
    return (
      <>
        <Hand size="XS" /> <Text>Script '{scriptName}'</Text>
      </>
    );
  } else {
    return <>{id}</>;
  }
};

export default ExecutableIdValue;
