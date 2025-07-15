import { Button, Flex, TextField, View } from '@adobe/react-spectrum';
import React, { useState } from 'react';

interface KeyValue {
  key: string;
  value: string;
}

interface KeyValueEditorProps {
  items: KeyValue[];
  onChange: (items: KeyValue[]) => void;
  uniqueKeys?: boolean;
}

const KeyValueEditor: React.FC<KeyValueEditorProps> = ({ items, onChange, uniqueKeys }) => {
  const [newKey, setNewKey] = useState('');
  const [newValue, setNewValue] = useState('');

  const addItem = () => {
    if (!newKey) return;
    if (uniqueKeys && items.some((item) => item.key === newKey)) return;
    onChange([...items, { key: newKey, value: newValue }]);
    setNewKey('');
    setNewValue('');
  };

  const removeItem = (idx: number) => {
    onChange(items.filter((_, i) => i !== idx));
  };

  const updateItem = (idx: number, key: string, value: string) => {
    const updated = items.map((item, i) => (i === idx ? { key, value } : item));
    onChange(updated);
  };

  return (
    <View>
      {items.map((item, idx) => {
        const isDuplicate = uniqueKeys && items.some((it, i) => it.key === item.key && i !== idx);
        return (
          <Flex key={idx} direction="row" gap="size-100" alignItems="end" marginBottom="size-100">
            <TextField label="Key" value={item.key} onChange={(k) => updateItem(idx, k, item.value)} width="size-2000" errorMessage={isDuplicate ? 'Duplicate key' : undefined} validationState={isDuplicate ? 'invalid' : undefined} />
            <TextField label="Value" value={item.value} onChange={(v) => updateItem(idx, item.key, v)} width="size-3000" />
            <Button variant="negative" onPress={() => removeItem(idx)}>
              Remove
            </Button>
          </Flex>
        );
      })}
      <Flex direction="row" gap="size-100" alignItems="end">
        <TextField
          label="Key"
          value={newKey}
          onChange={setNewKey}
          width="size-2000"
          errorMessage={uniqueKeys && items.some((item) => item.key === newKey) ? 'Duplicate key' : undefined}
          validationState={uniqueKeys && items.some((item) => item.key === newKey) ? 'invalid' : undefined}
        />
        <TextField label="Value" value={newValue} onChange={setNewValue} width="size-3000" />
        <Button variant="primary" onPress={addItem} isDisabled={!newKey || (uniqueKeys && items.some((item) => item.key === newKey))}>
          Add
        </Button>
      </Flex>
    </View>
  );
};

export default KeyValueEditor;
