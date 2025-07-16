import { Button, Grid, Text, TextField, View } from '@adobe/react-spectrum';
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

  // Define grid areas for all rows
  const areas = ['key value action', ...items.map((_, i) => `key${i} value${i} action${i}`), 'keyNew valueNew actionNew'];
  const rows = ['auto', ...items.map(() => 'auto'), 'auto'];

  return (
    <Grid areas={areas} columns={[KEY_COL_WIDTH, VALUE_COL_WIDTH, ACTION_COL_WIDTH]} rows={rows} gap="size-100">
      <View gridArea="key">
        <Text>{keyLabel}</Text>
      </View>
      <View gridArea="value">
        <Text>{valueLabel}</Text>
      </View>
      <View gridArea="action" />

      {items.map((item, idx) => {
        const isDuplicate = uniqueKeys && items.some((it, i) => it.key === item.key && i !== idx);
        return (
          <React.Fragment key={idx}>
            <TextField
              gridArea={`key${idx}`}
              aria-label={keyLabel}
              value={item.key}
              onChange={(k) => updateItem(idx, k, item.value)}
              errorMessage={isDuplicate ? 'Duplicate key' : undefined}
              validationState={isDuplicate ? 'invalid' : undefined}
              description=" "
              width="100%"
            />
            <TextField gridArea={`value${idx}`} aria-label={valueLabel} value={item.value} onChange={(v) => updateItem(idx, item.key, v)} description=" " width="100%" />
            <View gridArea={`action${idx}`}>
              <Button variant="negative" onPress={() => removeItem(idx)} aria-label="Remove" isQuiet>
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
        errorMessage={uniqueKeys && items.some((item) => item.key === newKey) ? 'Duplicate key' : undefined}
        validationState={uniqueKeys && items.some((item) => item.key === newKey) ? 'invalid' : undefined}
        description=" "
        width="100%"
      />
      <TextField gridArea="valueNew" aria-label={valueLabel} value={newValue} onChange={setNewValue} description=" " width="100%" />
      <View gridArea="actionNew">
        <Button variant="primary" onPress={addItem} isDisabled={!newKey || (uniqueKeys && items.some((item) => item.key === newKey))} aria-label="Add" isQuiet>
          <Add />
        </Button>
      </View>
    </Grid>
  );
};

export default KeyValueEditor;
