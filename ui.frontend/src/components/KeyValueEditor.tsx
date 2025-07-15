import { Button, Flex, Text, TextField, View } from '@adobe/react-spectrum';
import Add from '@spectrum-icons/workflow/Add';
import Delete from '@spectrum-icons/workflow/Delete';
import React, { useState } from 'react';

interface KeyValue {
  key: string;
  value: string;
}

interface KeyValueEditorProps {
  items: KeyValue[];
  onChange: (items: KeyValue[]) => void;
  uniqueKeys?: boolean;
  keyLabel?: string;
  valueLabel?: string;
}

const KEY_COL_WIDTH = 'size-2000';
const VALUE_COL_WIDTH = 'size-3000';
const ACTION_COL_WIDTH = 'size-600';

const KeyValueEditor: React.FC<KeyValueEditorProps> = ({ items, onChange, uniqueKeys, keyLabel = 'Key', valueLabel = 'Value' }) => {
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
    <Flex direction="column" gap="size-100">
      <Flex direction="row" alignItems="center" gap="size-100">
        <View width={KEY_COL_WIDTH}>
          <Text>{keyLabel}</Text>
        </View>
        <View width={VALUE_COL_WIDTH}>
          <Text>{valueLabel}</Text>
        </View>
        <View width={ACTION_COL_WIDTH} />
      </Flex>
      {items.map((item, idx) => {
        const isDuplicate = uniqueKeys && items.some((it, i) => it.key === item.key && i !== idx);
        return (
          <Flex key={idx} direction="row" alignItems="end" gap="size-100">
            <View width={KEY_COL_WIDTH}>
              <TextField aria-label={keyLabel} value={item.key} onChange={(k) => updateItem(idx, k, item.value)} errorMessage={isDuplicate ? 'Duplicate key' : undefined} validationState={isDuplicate ? 'invalid' : undefined} width="100%" />
            </View>
            <View width={VALUE_COL_WIDTH}>
              <TextField aria-label={valueLabel} value={item.value} onChange={(v) => updateItem(idx, item.key, v)} width="100%" />
            </View>
            <View width={ACTION_COL_WIDTH}>
              <Button variant="negative" onPress={() => removeItem(idx)} aria-label="Remove" isQuiet>
                <Delete />
              </Button>
            </View>
          </Flex>
        );
      })}
      <Flex direction="row" alignItems="end" gap="size-100">
        <View width={KEY_COL_WIDTH}>
          <TextField
            aria-label={keyLabel}
            value={newKey}
            onChange={setNewKey}
            errorMessage={uniqueKeys && items.some((item) => item.key === newKey) ? 'Duplicate key' : undefined}
            validationState={uniqueKeys && items.some((item) => item.key === newKey) ? 'invalid' : undefined}
            width="100%"
          />
        </View>
        <View width={VALUE_COL_WIDTH}>
          <TextField aria-label={valueLabel} value={newValue} onChange={setNewValue} width="100%" />
        </View>
        <View width={ACTION_COL_WIDTH}>
          <Button variant="primary" onPress={addItem} isDisabled={!newKey || (uniqueKeys && items.some((item) => item.key === newKey))} aria-label="Add" isQuiet>
            <Add />
          </Button>
        </View>
      </Flex>
    </Flex>
  );
};

export default KeyValueEditor;
