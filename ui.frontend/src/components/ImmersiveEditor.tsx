import { Button, Content, Dialog, DialogContainer, Divider, Flex, View, ViewProps } from '@adobe/react-spectrum';
import { Editor, EditorProps, useMonaco } from '@monaco-editor/react';
import { ColorVersion } from '@react-types/shared';
import FullScreenExit from '@spectrum-icons/workflow/FullScreenExit';
import { MarkerSeverity } from 'monaco-editor';
import { useCallback, useEffect, useState } from 'react';

export type SyntaxError = { line: number; column: number; message: string };
type ImmersiveEditorProps<C extends ColorVersion> = EditorProps & { containerProps?: ViewProps<C>; syntaxError?: SyntaxError };

const ImmersiveEditor = <C extends ColorVersion>({ containerProps, syntaxError, ...props }: ImmersiveEditorProps<C>) => {
  const [isOpen, setIsOpen] = useState(false);
  const monacoRef = useMonaco();

  const updateMarkers = useCallback(() => {
    if (monacoRef?.editor) {
      const models = monacoRef.editor.getModels();

      models.forEach((model) =>
        monacoRef?.editor.setModelMarkers(
          model,
          model?.id,
          syntaxError
            ? [
                {
                  startLineNumber: syntaxError.line,
                  startColumn: syntaxError.column,
                  endLineNumber: syntaxError.line,
                  endColumn: syntaxError.column + 10,
                  message: syntaxError.message,
                  severity: MarkerSeverity.Error,
                },
              ]
            : [],
        ),
      );
    }
  }, [monacoRef?.editor, syntaxError]);

  useEffect(() => {
    updateMarkers();
  }, [syntaxError, updateMarkers]);

  return (
    <View backgroundColor="gray-800" borderWidth="thin" position="relative" borderColor="dark" height="100%" borderRadius="medium" padding="size-50" {...containerProps}>
      {!isOpen && (
        <>
          <Editor theme="vs-dark" height="100%" onMount={() => updateMarkers()} {...props} />
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
                  <Editor theme="vs-dark" height="100%" onMount={() => updateMarkers()} {...props} />
                </View>
              </Flex>
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
