import { Button, Content, Dialog, DialogContainer, Divider, Flex, ProgressCircle, View, ViewProps } from '@adobe/react-spectrum';
import Editor, { EditorProps } from '@monaco-editor/react';
import { ColorVersion } from '@react-types/shared';
import FullScreenExit from '@spectrum-icons/workflow/FullScreenExit';
import { useState } from 'react';

type ImmersiveEditorProps<C extends ColorVersion> = EditorProps & { containerProps?: ViewProps<C>; inProgress?: boolean };

const ImmersiveEditor = <C extends ColorVersion>({ containerProps, inProgress, ...props }: ImmersiveEditorProps<C>) => {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <View backgroundColor="gray-800" borderWidth="thin" position="relative" borderColor="dark" height="100%" borderRadius="medium" padding="size-50" {...containerProps}>
      {!isOpen && (
        <>
          <Editor theme="vs-dark" height="100%" {...props} />
          {inProgress && <ProgressCircle staticColor="white" position="absolute" zIndex={5} bottom={10} left={10} isIndeterminate aria-label="Parsing pending...." />}
          <Button variant="primary" style="fill" position="absolute" zIndex={5} bottom={10} right={10} onPress={() => setIsOpen(true)}>
            <FullScreenExit />
          </Button>
        </>
      )}
      <DialogContainer onDismiss={() => setIsOpen(false)} type="fullscreenTakeover">
        {isOpen && (
          <Dialog marginTop={8}>
            <Content gridColumn="1 / span 5" gridRow="2 / span 4" height="100%">
              <Flex height="100%" direction="column">
                <Divider size="M" orientation="horizontal" />
                <View backgroundColor="gray-800" paddingTop={10} height="100%">
                  <Editor theme="vs-dark" height="100%" {...props} />
                </View>
              </Flex>
            </Content>
            {inProgress && <ProgressCircle staticColor="white" position="absolute" zIndex={5} bottom={10} left={10} isIndeterminate aria-label="Parsing pending...." />}
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
