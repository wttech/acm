import { Checkbox, CheckboxGroup, Item, ListView, NumberField, Picker, Radio, RadioGroup, Switch, TextArea, TextField, View } from '@adobe/react-spectrum';
import { Editor } from '@monaco-editor/react';
import { Field } from '@react-spectrum/label';
import React from 'react';
import { Argument, ArgumentValue, isBoolArgument, isMultiSelectArgument, isNumberArgument, isSelectArgument, isStringArgument, isTextArgument } from '../utils/api.types.ts';
import { Strings } from '../utils/strings.ts';

interface CodeArgumentInputProps {
  arg: Argument<ArgumentValue>;
  value: ArgumentValue;
  onChange: (name: string, value: ArgumentValue) => void;
}

const CodeArgumentInput: React.FC<CodeArgumentInputProps> = ({ arg, value, onChange }) => {
  if (isBoolArgument(arg)) {
    return (
      <View key={arg.name} marginBottom="size-200">
        {arg.display === 'SWITCHER' ? (
          <Switch aria-label={`Argument "${arg.name}"`} isSelected={value as boolean} onChange={(val) => onChange(arg.name, val)}>
            {argLabel(arg)}
          </Switch>
        ) : (
          <Checkbox aria-label={`Argument "${arg.name}"`} isSelected={value as boolean} onChange={(val) => onChange(arg.name, val)}>
            {argLabel(arg)}
          </Checkbox>
        )}
      </View>
    );
  } else if (isStringArgument(arg)) {
    return (
      <View key={arg.name} marginBottom="size-200">
        <TextField aria-label={`Argument "${arg.name}"`} width="100%" label={argLabel(arg)} value={value?.toString() || ''} onChange={(val) => onChange(arg.name, val)} />
      </View>
    );
  } else if (isTextArgument(arg)) {
    return (
      <View key={arg.name} marginY="size-100">
        {arg.language ? (
          <>
            <Field label={argLabel(arg)} description={`Language: ${arg.language}`} width="100%">
              <div>
                <View width="100%" backgroundColor="gray-800" borderWidth="thin" position="relative" borderColor="dark" height="100%" borderRadius="medium" padding="size-50">
                  <Editor language={arg.language} theme="vs-dark" height="200px" options={{ scrollBeyondLastLine: false }} value={value?.toString() || ''} onChange={(val) => onChange(arg.name, val || '')} />
                </View>
              </div>
            </Field>
          </>
        ) : (
          <TextArea aria-label={`Argument "${arg.name}"`} width="100%" label={argLabel(arg)} value={value?.toString() || ''} onChange={(val) => onChange(arg.name, val)} />
        )}
      </View>
    );
  } else if (isSelectArgument(arg)) {
    const display = arg.display === 'AUTO' ? (Object.entries(arg.options).length <= 3 ? 'RADIO' : 'DROPDOWN') : arg.display;
    return (
      <View key={arg.name} marginBottom="size-200">
        {display === 'RADIO' ? (
          <RadioGroup orientation="horizontal" aria-label={`Argument "${arg.name}"`} label={argLabel(arg)} value={value?.toString() || ''} onChange={(val) => onChange(arg.name, val)}>
            {Object.entries(arg.options).map(([label, val]) => (
              <Radio key={val?.toString()} value={val?.toString() || ''}>
                {label}
              </Radio>
            ))}
          </RadioGroup>
        ) : (
          <Picker aria-label={`Argument "${arg.name}"`} width="100%" label={argLabel(arg)} selectedKey={value?.toString() || ''} onSelectionChange={(val) => onChange(arg.name, val)}>
            {Object.entries(arg.options).map(([label, val]) => (
              <Item key={val?.toString()}>{label}</Item>
            ))}
          </Picker>
        )}
      </View>
    );
  } else if (isMultiSelectArgument(arg)) {
    const display = arg.display === 'AUTO' ? (Object.entries(arg.options).length <= 3 ? 'CHECKBOX' : 'LIST') : arg.display;
    return (
      <View key={arg.name} marginBottom="size-200">
        {display === 'CHECKBOX' ? (
          <CheckboxGroup orientation="horizontal" aria-label={`Argument "${arg.name}"`} label={argLabel(arg)} value={value as string[]} onChange={(val) => onChange(arg.name, val)}>
            {Object.entries(arg.options).map(([label, val]) => (
              <Checkbox key={val?.toString()} value={val?.toString() || ''}>
                {label}
              </Checkbox>
            ))}
          </CheckboxGroup>
        ) : (
          <Field label={argLabel(arg)} width="100%">
            <div>
              <ListView aria-label={`Argument "${arg.name}"`} maxHeight="size-1600" selectionMode="multiple" selectedKeys={value as string[]} onSelectionChange={(val) => onChange(arg.name, Array.from(val as Set<string>))}>
                {Object.entries(arg.options).map(([label, val]) => (
                  <Item key={val?.toString()}>{label}</Item>
                ))}
              </ListView>
            </div>
          </Field>
        )}
      </View>
    );
  } else if (isNumberArgument(arg)) {
    return (
      <View key={arg.name} marginBottom="size-200">
        <NumberField
          aria-label={`Argument "${arg.name}"`}
          label={argLabel(arg)}
          value={value as number}
          onChange={(val) => onChange(arg.name, val)}
          minValue={arg.min !== null ? arg.min : undefined}
          maxValue={arg.max !== null ? arg.max : undefined}
          hideStepper={arg.type === 'DECIMAL'}
          formatOptions={arg.type === 'INTEGER' ? { maximumFractionDigits: 0 } : undefined}
        />
      </View>
    );
  } else {
    return null;
  }
};

function argLabel(arg: Argument<ArgumentValue>): string {
  if (arg.label) {
    return arg.label;
  }
  return Strings.capitalizeWords(arg.name);
}

export default CodeArgumentInput;
