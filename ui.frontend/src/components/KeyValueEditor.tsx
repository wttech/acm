import { Button, Grid, Text, TextField, View } from '@adobe/react-spectrum';
import Add from '@spectrum-icons/workflow/Add';
import Delete from '@spectrum-icons/workflow/Delete';
import React, { useEffect, useState } from 'react';

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

const KeyValueEditor: React.FC<KeyValueEditorProps> = ({ items, onChange, uniqueKeys, keyLabel = 'Key', valueLabel = 'Value' }) => {
  const [localItems, setLocalItems] = useState<KeyValue[]>(items);
  const [newKey, setNewKey] = useState('');
  const [newValue, setNewValue] = useState('');

  useEffect(() => {
    setLocalItems(items);
  }, [items]);

  const hasDuplicates = (arr: KeyValue[]) => {
    const keys = arr.map((item) => item.key);
    return new Set(keys).size !== keys.length;
  };

  const tryUpdate = (updated: KeyValue[]) => {
    setLocalItems(updated);
    if (!uniqueKeys || !hasDuplicates(updated)) {
      onChange(updated);
    }
  };

  const addItem = () => {
    if (!newKey) return;
    const updated = [...localItems, { key: newKey, value: newValue }];
    tryUpdate(updated);
    setNewKey('');
    setNewValue('');
  };

  const removeItem = (idx: number) => {
    const updated = localItems.filter((_, i) => i !== idx);
    tryUpdate(updated);
  };

  const updateItem = (idx: number, key: string, value: string) => {
    const updated = localItems.map((item, i) => (i === idx ? { key, value } : item));
    tryUpdate(updated);
  };

  const areas = ['key value action', ...localItems.map((_, i) => `key${i} value${i} action${i}`), 'keyNew valueNew actionNew'];
  const rows = ['auto', ...localItems.map(() => 'auto'), 'auto'];

  return (
    <Grid areas={areas} columns={['1fr', '2fr']} rows={rows} rowGap="size-50" columnGap="size-100">
      <View gridArea="key">
        <Text>{keyLabel}</Text>
      </View>
      <View gridArea="value">
        <Text>{valueLabel}</Text>
      </View>
      <View gridArea="action" />

      {localItems.map((item, idx) => {
        const isDuplicate = uniqueKeys && localItems.some((it, i) => it.key === item.key && i !== idx);
        return (
          <React.Fragment key={idx}>
            <TextField
              gridArea={`key${idx}`}
              aria-label={keyLabel}
              value={item.key}
              onChange={(k) => updateItem(idx, k, item.value)}
              errorMessage={isDuplicate ? 'Duplicate key' : undefined}
              validationState={isDuplicate ? 'invalid' : undefined}
              width="100%"
            />
            <TextField gridArea={`value${idx}`} aria-label={valueLabel} value={item.value} onChange={(v) => updateItem(idx, item.key, v)} width="100%" />
            <View gridArea={`action${idx}`}>
              <Button variant="negative" onPress={() => removeItem(idx)} aria-label="Remove">
                <Delete />
              </Button>
            </View>
          </React.Fragment>
        );
      })}

      <TextField
        gridArea="keyNew"
        aria-label={keyLabel}
        value={newKey}
        onChange={setNewKey}
        errorMessage={uniqueKeys && localItems.some((item) => item.key === newKey) && newKey ? 'Duplicate key' : undefined}
        validationState={uniqueKeys && localItems.some((item) => item.key === newKey) && newKey ? 'invalid' : undefined}
        width="100%"
      />
      <TextField gridArea="valueNew" aria-label={valueLabel} value={newValue} onChange={setNewValue} width="100%" />
      <View gridArea="actionNew">
        <Button variant="primary" onPress={addItem} isDisabled={!newKey || (uniqueKeys && localItems.some((item) => item.key === newKey))} aria-label="Add">
          <Add />
        </Button>
      </View>
    </Grid>
  );
};

export default KeyValueEditor;
