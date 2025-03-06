import { Button, Content, Dialog, DialogContainer, Divider, Flex, View, ViewProps } from '@adobe/react-spectrum';
import { useMonaco } from '@monaco-editor/react';
import { ColorVersion } from '@react-types/shared';
import FullScreenExit from '@spectrum-icons/workflow/FullScreenExit';
import { MarkerSeverity, editor } from 'monaco-editor';
import { useEffect, useRef, useState } from 'react';
import { debounce } from '../utils/debounce.ts';
import { modelStorage } from '../utils/modelStorage.ts';
import { registerGroovyLanguage } from '../utils/monaco/groovy.ts';

export type SyntaxError = { line: number; column: number; message: string };
type ImmersiveEditorProps<C extends ColorVersion> = editor.IStandaloneEditorConstructionOptions & { id: string; initialValue?: string; containerProps?: ViewProps<C>; syntaxError?: SyntaxError; onChange?: (code: string) => void };

const saveViewStateDebounce = 1000;

const ImmersiveEditor = <C extends ColorVersion>({ containerProps, syntaxError, onChange, id, language, value, initialValue, ...props }: ImmersiveEditorProps<C>) => {
  const [isOpen, setIsOpen] = useState(false);
  const monacoRef = useMonaco();
  const containerRef = useRef<HTMLDivElement>(null);
  const debouncedViewStateUpdate = debounce((mountedEditor: editor.IStandaloneCodeEditor) => {
    modelStorage.updateViewState(id, mountedEditor.saveViewState());
  }, saveViewStateDebounce);

  useEffect(() => {
    if (value) {
      modelStorage.getModel(id)?.textModel.setValue(value);
    }
  }, [id, monacoRef?.editor, value]);

  useEffect(() => {
    if (!containerRef.current || !monacoRef) {
      return;
    }

    if (language === 'groovy') {
      registerGroovyLanguage(monacoRef);
    }

    const storedModel = modelStorage.getModel(id);
    const textModel = storedModel?.textModel || monacoRef.editor.createModel(initialValue ?? value ?? '', language);

    const mountedEditor = monacoRef.editor.create(containerRef.current, {
      model: textModel,
      theme: 'vs-dark',
      value: initialValue ?? value,
      ...props,
    });

    if (storedModel?.viewState) {
      mountedEditor.restoreViewState(storedModel.viewState);
    }

    const changeListener = textModel.onDidChangeContent(() => {
      debouncedViewStateUpdate(mountedEditor);
      onChange?.(mountedEditor.getValue());
    });

    modelStorage.updateModel(id, textModel, mountedEditor.saveViewState());

    mountedEditor.focus();

    return () => {
      mountedEditor.dispose();
      changeListener?.dispose();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [monacoRef, isOpen]);

  useEffect(() => {
    const textModel = modelStorage.getModel(id)?.textModel;

    if (monacoRef?.editor && textModel) {
      monacoRef?.editor.setModelMarkers(
        textModel,
        id,
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
  }, [id, monacoRef?.editor, syntaxError]);

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
