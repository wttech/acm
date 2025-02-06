import { Button, Content, Dialog, DialogContainer, View, ViewProps } from '@adobe/react-spectrum';
import Editor, { EditorProps } from '@monaco-editor/react';
import { ColorVersion } from '@react-types/shared';
import FullScreenExit from '@spectrum-icons/workflow/FullScreenExit';
import { useState } from 'react';

const ImmersiveEditor = <C extends ColorVersion>({ containerProps, ...props }: EditorProps & { containerProps?: ViewProps<C> }) => {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <View backgroundColor="gray-800" borderWidth="thin" position="relative" borderColor="dark" height="100%" borderRadius="medium" padding="size-50" {...containerProps}>
      {!isOpen && <Editor {...props} />}
      <Button variant="primary" style="fill" position="absolute" zIndex={5} bottom={10} right={10} onPress={() => setIsOpen(true)}>
        <FullScreenExit />
      </Button>
      <DialogContainer onDismiss={() => setIsOpen(false)} type="fullscreenTakeover">
        {isOpen && (
          <Dialog marginTop={8}>
            <Content gridColumn="span 5" gridRow="2 / span 4">
              {isOpen && <Editor {...props} />}
            </Content>
            <Button variant="primary" style="fill" position="absolute" zIndex={5} bottom={10} right={10} onPress={() => setIsOpen(false)}>
              <FullScreenExit />
            </Button>
          </Dialog>
        )}
      </DialogContainer>
    </View>
  );
};

export default ImmersiveEditor;
