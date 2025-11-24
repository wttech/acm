import { View } from '@adobe/react-spectrum';
import { Editor, EditorProps } from '@monaco-editor/react';
import React from 'react';

const CodeTextarea: React.FC<EditorProps> = ({ options, ...props }) => {
  return (
    <View width="100%" backgroundColor="gray-800" borderWidth="thin" position="relative" borderColor="dark" height="100%" borderRadius="medium" padding="size-50">
      <Editor
        theme="vs-dark"
        height="160px"
        options={{
          scrollBeyondLastLine: false,
          ...options,
        }}
        {...props}
      />
    </View>
  );
};

export default CodeTextarea;
