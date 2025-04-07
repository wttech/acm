import { Checkbox, CheckboxGroup, Item, ListView, NumberField, Picker, Radio, RadioGroup, Switch, TextArea, TextField, View } from '@adobe/react-spectrum';
import { Editor } from '@monaco-editor/react';
import { Field } from '@react-spectrum/label';
import React from 'react';
import { Controller, useFormContext } from 'react-hook-form';
import { Argument, ArgumentValue, isBoolArgument, isMultiSelectArgument, isNumberArgument, isSelectArgument, isStringArgument, isTextArgument } from '../utils/api.types.ts';
import { Strings } from '../utils/strings.ts';

interface CodeArgumentInputProps {
  arg: Argument<ArgumentValue>;
  value: ArgumentValue;
  onChange: (name: string, value: ArgumentValue) => void;
}

const CodeArgumentInput: React.FC<CodeArgumentInputProps> = ({ arg }) => {
  const { control } = useFormContext();

  const requiredRule = { required: { value: true, message: 'Value is required' } };

  const renderInput = () => {
    if (isBoolArgument(arg)) {
      return (
        <Controller
          name={arg.name}
          control={control}
          render={({ field }) => (
            <View key={arg.name} marginBottom="size-200">
              {arg.display === 'SWITCHER' ? (
                <Switch {...field} isSelected={field.value} onChange={field.onChange} aria-label={`Argument '${arg.name}'`}>
                  {argLabel(arg)}
                </Switch>
              ) : (
                <Checkbox {...field} isSelected={field.value} onChange={field.onChange} aria-label={`Argument '${arg.name}'`}>
                  {argLabel(arg)}
                </Checkbox>
              )}
            </View>
          )}
        />
      );
    } else if (isStringArgument(arg)) {
      return (
        <Controller
          name={arg.name}
          control={control}
          rules={arg.required ? requiredRule : undefined}
          render={({ field, fieldState }) => (
            <View key={arg.name} marginBottom="size-200">
              <TextField {...field} label={argLabel(arg)} errorMessage={fieldState.error ? fieldState.error.message : undefined} validationState={fieldState.error ? 'invalid' : 'valid'} aria-label={`Argument '${arg.name}'`} width="100%" />
            </View>
          )}
        />
      );
    } else if (isTextArgument(arg)) {
      return (
        <Controller
          name={arg.name}
          control={control}
          rules={arg.required ? requiredRule : undefined}
          render={({ field, fieldState }) => (
            <View key={arg.name} marginY="size-100">
              {arg.language ? (
                <Field label={argLabel(arg)} description={`Language: ${arg.language}`} width="100%">
                  <div>
                    <View width="100%" backgroundColor="gray-800" borderWidth="thin" position="relative" borderColor="dark" height="100%" borderRadius="medium" padding="size-50">
                      <Editor
                        aria-label={`Argument '${arg.name}'`}
                        /*{...field}*/ /* TODO onBlur here */
                        language={arg.language}
                        theme="vs-dark"
                        height="200px"
                        options={{ scrollBeyondLastLine: false }}
                        value={field.value?.toString() || ''}
                        onChange={(val) => field.onChange(val || '')}
                      />
                    </View>
                  </div>
                </Field>
              ) : (
                <TextArea {...field} label={argLabel(arg)} errorMessage={fieldState.error ? fieldState.error.message : undefined} validationState={fieldState.error ? 'invalid' : 'valid'} aria-label={`Argument '${arg.name}'`} width="100%" />
              )}
            </View>
          )}
        />
      );
    } else if (isSelectArgument(arg)) {
      const display = arg.display === 'AUTO' ? (Object.entries(arg.options).length <= 3 ? 'RADIO' : 'DROPDOWN') : arg.display;
      return (
        <Controller
          name={arg.name}
          control={control}
          rules={arg.required ? requiredRule : undefined}
          render={({ field, fieldState }) => (
            <View key={arg.name} marginBottom="size-200">
              {display === 'RADIO' ? (
                <RadioGroup
                  {...field}
                  orientation="horizontal"
                  label={argLabel(arg)}
                  errorMessage={fieldState.error ? fieldState.error.message : undefined}
                  validationState={fieldState.error ? 'invalid' : 'valid'}
                  aria-label={`Argument '${arg.name}'`}
                >
                  {Object.entries(arg.options).map(([label, val]) => (
                    <Radio key={val?.toString()} value={val?.toString() || ''}>
                      {label}
                    </Radio>
                  ))}
                </RadioGroup>
              ) : (
                <Picker
                  {...field}
                  label={argLabel(arg)}
                  selectedKey={field.value?.toString() || ''}
                  onSelectionChange={field.onChange}
                  errorMessage={fieldState.error ? fieldState.error.message : undefined}
                  validationState={fieldState.error ? 'invalid' : 'valid'}
                  aria-label={`Argument '${arg.name}'`}
                >
                  {Object.entries(arg.options).map(([label, val]) => (
                    <Item key={val?.toString()}>{label}</Item>
                  ))}
                </Picker>
              )}
            </View>
          )}
        />
      );
    } else if (isMultiSelectArgument(arg)) {
      const display = arg.display === 'AUTO' ? (Object.entries(arg.options).length <= 3 ? 'CHECKBOX' : 'LIST') : arg.display;
      return (
        <Controller
          name={arg.name}
          control={control}
          rules={arg.required ? requiredRule : undefined}
          render={({ field, fieldState }) => (
            <View key={arg.name} marginBottom="size-200">
              {display === 'CHECKBOX' ? (
                <CheckboxGroup
                  {...field}
                  orientation="horizontal"
                  label={argLabel(arg)}
                  errorMessage={fieldState.error ? fieldState.error.message : undefined}
                  validationState={fieldState.error ? 'invalid' : 'valid'}
                  aria-label={`Argument '${arg.name}'`}
                >
                  {Object.entries(arg.options).map(([label, val]) => (
                    <Checkbox key={val?.toString()} value={val?.toString() || ''}>
                      {label}
                    </Checkbox>
                  ))}
                </CheckboxGroup>
              ) : (
                <Field label={argLabel(arg)} width="100%">
                  <div>
                    <ListView
                      {...field}
                      maxHeight="size-1600"
                      selectionMode="multiple"
                      selectedKeys={field.value as string[]}
                      onSelectionChange={(val) => field.onChange(Array.from(val as Set<string>))}
                      aria-label={`Argument '${arg.name}'`}
                    >
                      {Object.entries(arg.options).map(([label, val]) => (
                        <Item key={val?.toString()}>{label}</Item>
                      ))}
                    </ListView>
                  </div>
                </Field>
              )}
            </View>
          )}
        />
      );
    } else if (isNumberArgument(arg)) {
      return (
        <Controller
          name={arg.name}
          control={control}
          rules={arg.required ? requiredRule : undefined}
          render={({ field, fieldState }) => (
            <View key={arg.name} marginBottom="size-200">
              <NumberField
                {...field}
                label={argLabel(arg)}
                errorMessage={fieldState.error ? fieldState.error.message : undefined}
                validationState={fieldState.error ? 'invalid' : 'valid'}
                minValue={arg.min !== null ? arg.min : undefined}
                maxValue={arg.max !== null ? arg.max : undefined}
                hideStepper={arg.type === 'DECIMAL'}
                formatOptions={arg.type === 'INTEGER' ? { maximumFractionDigits: 0 } : undefined}
                aria-label={`Argument '${arg.name}'`}
              />
            </View>
          )}
        />
      );
    } else {
      return null;
    }
  };

  return <>{renderInput()}</>;
};

function argLabel(arg: Argument<ArgumentValue>): string {
  if (arg.label) {
    return arg.label;
  }
  return Strings.capitalizeWords(arg.name);
}

export default CodeArgumentInput;

