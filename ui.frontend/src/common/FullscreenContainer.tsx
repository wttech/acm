import { Button, View, ViewProps } from '@adobe/react-spectrum';
import { ColorVersion } from '@react-types/shared';
import FullScreen from '@spectrum-icons/workflow/FullScreen';
import FullScreenExit from '@spectrum-icons/workflow/FullScreenExit';
import { useState } from 'react';

const FullscreenContainer = <C extends ColorVersion>({ children, ...rest }: ViewProps<C>) => {
  const [fullscreen, setFullscreen] = useState<boolean>(false);

  return (
    <View
      backgroundColor="gray-800"
      borderWidth="thin"
      position="relative"
      borderColor="dark"
      height="100%"
      borderRadius="medium"
      padding="size-50"
      overflow="hidden"
      {...(fullscreen && { position: 'fixed', left: 0, right: 0, top: 0, bottom: 0 })}
      {...rest}
    >
      {children}
      <Button variant="primary" style="fill" position="absolute" zIndex={9999999} bottom={10} right={10} onPress={() => setFullscreen((prev) => !prev)}>
        {fullscreen ? <FullScreenExit /> : <FullScreen />}
      </Button>
    </View>
  );
};

export default FullscreenContainer;
