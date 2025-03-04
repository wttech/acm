import { Button, Content, Dialog, DialogContainer, Divider, Flex, View, ViewProps } from '@adobe/react-spectrum';
import { useMonaco } from '@monaco-editor/react';
import { ColorVersion } from '@react-types/shared';
import FullScreenExit from '@spectrum-icons/workflow/FullScreenExit';
import { MarkerSeverity, editor } from 'monaco-editor';
import { useEffect, useRef, useState } from 'react';
import { modelStorage } from '../utils/modelStorage.ts';
import { registerGroovyLanguage } from '../utils/monaco/groovy.ts';

export type SyntaxError = { line: number; column: number; message: string };
type ImmersiveEditorProps<C extends ColorVersion> = editor.IStandaloneEditorConstructionOptions & { persistenceId?: string; containerProps?: ViewProps<C>; syntaxError?: SyntaxError; onChange?: (code: string) => void };

const ImmersiveEditor = <C extends ColorVersion>({ containerProps, syntaxError, onChange, persistenceId, language, value, ...props }: ImmersiveEditorProps<C>) => {
  const [isOpen, setIsOpen] = useState(false);
  const monacoRef = useMonaco();
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!containerRef.current || !monacoRef) {
      return;
    }

    if (language === 'groovy') {
      registerGroovyLanguage(monacoRef);
    }

    const storedModel = persistenceId ? modelStorage.getModel(persistenceId) : null;
    const textModel = storedModel?.textModel || monacoRef.editor.createModel(value ?? '', language);

    const mountedEditor = monacoRef.editor.create(containerRef.current, {
      model: textModel,
      theme: 'vs-dark',
      value,
      ...props,
    });

    if (storedModel?.viewState) {
      mountedEditor.restoreViewState(storedModel.viewState);
    }

    mountedEditor.focus();

    const changeListener = mountedEditor.onDidChangeCursorPosition(() => {
      if (persistenceId) {
        modelStorage.updateViewState(persistenceId, mountedEditor.saveViewState());
      }

      onChange?.(mountedEditor.getValue());
    });

    if (persistenceId) {
      modelStorage.updateModel(persistenceId, textModel, mountedEditor.saveViewState());
    }

    return () => {
      mountedEditor.dispose();
      changeListener?.dispose();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [monacoRef, isOpen]);

  useEffect(() => {
    const textModel = persistenceId ? modelStorage.getModel(persistenceId)?.textModel : null;

    if (monacoRef?.editor && persistenceId && textModel) {
      monacoRef?.editor.setModelMarkers(
        textModel,
        persistenceId,
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
      );
    }
  }, [persistenceId, monacoRef?.editor, syntaxError]);

  return (
    <View backgroundColor="gray-800" borderWidth="thin" position="relative" borderColor="dark" height="100%" borderRadius="medium" padding="size-50" {...containerProps}>
      {!isOpen && (
        <>
          <div ref={containerRef} style={{ height: '100%' }} />
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
                  <div ref={containerRef} style={{ height: '100%' }} />
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
