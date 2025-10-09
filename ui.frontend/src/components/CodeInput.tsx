import {
  Button,
  Checkbox,
  CheckboxGroup,
  ColorEditor,
  ColorFormat,
  ColorPicker,
  DateField,
  DatePicker,
  Item,
  ListView,
  NumberField,
  parseColor,
  Picker,
  Radio,
  RadioGroup,
  RangeSlider,
  Slider,
  Switch,
  TextArea,
  TextField,
  TimeField,
  View,
} from '@adobe/react-spectrum';
import { Field } from '@react-spectrum/label';
import React from 'react';
import { Controller } from 'react-hook-form';
import { useInput } from '../hooks/form.ts';
import {
  Input,
  InputValue,
  isBoolInput,
  isColorInput,
  isDateTimeInput,
  isFileInput,
  isKeyValueListInput,
  isMapInput,
  isMultiFileInput,
  isMultiSelectInput,
  isNumberInput,
  isPathInput,
  isRangeInput,
  isSelectInput,
  isStringInput,
  isTextInput,
} from '../types/input.ts';
import { Dates } from '../utils/dates.ts';
import { Strings } from '../utils/strings.ts';
import CodeTextarea from './CodeTextarea.tsx';
import FileUploader from './FileUploader.tsx';
import KeyValueEditor from './KeyValueEditor.tsx';
import Markdown from './Markdown.tsx';
import PathField from './PathPicker.tsx';

interface CodeInputProps {
  input: Input<InputValue>;
  value: InputValue;
  onChange: (name: string, value: InputValue) => void;
}

const CodeInput: React.FC<CodeInputProps> = ({ input }) => {
  const { control, controllerRules } = useInput(input);
  const label = input.label || Strings.capitalizeWords(input.name);
  const description = input.description ? <Markdown code={input.description} /> : undefined;

  return (
    <Controller
      name={input.name}
      control={control}
      rules={controllerRules}
      render={({ field, fieldState }) => (
        <View key={input.name} marginBottom="size-150" marginTop="size-0">
          {(() => {
            if (isBoolInput(input)) {
              return (
                <Field description={description} width="100%" errorMessage={fieldState.error ? fieldState.error.message : undefined} validationState={fieldState.error ? 'invalid' : 'valid'}>
                  <div>
                    {input.display === 'SWITCHER' ? (
                      <Switch {...field} isSelected={field.value} onChange={field.onChange} aria-label={`Input '${input.name}'`}>
                        {label}
                      </Switch>
                    ) : (
                      <Checkbox {...field} isSelected={field.value} isInvalid={!!fieldState.error} onChange={field.onChange} aria-label={`Input '${input.name}'`}>
                        {label}
                      </Checkbox>
                    )}
                  </div>
                </Field>
              );
            } else if (isDateTimeInput(input)) {
              return input.type === 'DATE' ? (
                <DateField
                  {...field}
                  value={Dates.toCalendarDateOrNull(field.value)}
                  minValue={input.min !== null ? Dates.toCalendarDate(input.min) : undefined}
                  maxValue={input.max !== null ? Dates.toCalendarDate(input.max) : undefined}
                  onChange={(dateValue) => field.onChange(dateValue?.toString())}
                  label={label}
                  {...(fieldState.error && {
                    validationState: 'invalid',
                    errorMessage: fieldState.error.message,
                  })}
                  description={description}
                  aria-label={`Input '${input.name}'`}
                />
              ) : input.type === 'TIME' ? (
                <TimeField
                  {...field}
                  value={Dates.toTimeOrNull(field.value)}
                  minValue={input.min !== null ? Dates.toTime(input.min) : undefined}
                  maxValue={input.max !== null ? Dates.toTime(input.max) : undefined}
                  onChange={(timeValue) => field.onChange(timeValue?.toString())}
                  label={label}
                  {...(fieldState.error && {
                    validationState: 'invalid',
                    errorMessage: fieldState.error.message,
                  })}
                  description={description}
                  aria-label={`Input '${input.name}'`}
                />
              ) : (
                <DatePicker
                  {...field}
                  value={Dates.toCalendarDateTimeOrNull(field.value)}
                  minValue={input.min !== null ? Dates.toCalendarDateTime(input.min) : undefined}
                  maxValue={input.max !== null ? Dates.toCalendarDateTime(input.max) : undefined}
                  onChange={(dateValue) => field.onChange(dateValue?.toString())}
                  granularity="second"
                  label={label}
                  {...(fieldState.error && {
                    validationState: 'invalid',
                    errorMessage: fieldState.error.message,
                  })}
                  description={description}
                  aria-label={`Input '${input.name}'`}
                />
              );
            } else if (isStringInput(input)) {
              return (
                <TextField
                  type={input.display}
                  {...field}
                  value={field.value ?? ''}
                  label={label}
                  description={description}
                  errorMessage={fieldState.error ? fieldState.error.message : undefined}
                  {...(fieldState.error && { validationState: 'invalid' })}
                  isRequired={input.required}
                  aria-label={`Input '${input.name}'`}
                  width={input.display !== 'PASSWORD' ? '100%' : undefined}
                />
              );
            } else if (isTextInput(input)) {
              return input.language ? (
                <Field
                  label={label}
                  description={description}
                  width="100%"
                  {...(fieldState.error && {
                    validationState: 'invalid',
                    errorMessage: fieldState.error.message,
                  })}
                >
                  <div>
                    <CodeTextarea aria-label={`Input '${input.name}'`} language={input.language} value={field.value?.toString() || ''} onChange={field.onChange} />
                  </div>
                </Field>
              ) : (
                <TextArea
                  {...field}
                  value={field.value ?? ''}
                  label={label}
                  {...(fieldState.error && {
                    validationState: 'invalid',
                    errorMessage: fieldState.error.message,
                  })}
                  aria-label={`Input '${input.name}'`}
                  width="100%"
                />
              );
            } else if (isSelectInput(input)) {
              const display = input.display === 'AUTO' ? (Object.entries(input.options).length <= 3 ? 'RADIO' : 'DROPDOWN') : input.display;
              return (
                <View key={input.name} marginBottom="size-200">
                  {display === 'RADIO' ? (
                    <RadioGroup
                      value={field.value}
                      onChange={field.onChange}
                      onBlur={field.onBlur}
                      orientation="horizontal"
                      label={label}
                      description={description}
                      errorMessage={fieldState.error ? fieldState.error.message : undefined}
                      validationState={fieldState.error ? 'invalid' : 'valid'}
                      aria-label={`Input '${input.name}'`}
                    >
                      {Object.entries(input.options).map(([label, val]) => (
                        <Radio key={val?.toString()} value={val?.toString() || ''}>
                          {label}
                        </Radio>
                      ))}
                    </RadioGroup>
                  ) : (
                    <Picker
                      label={label}
                      description={description}
                      selectedKey={field.value?.toString() || ''}
                      onSelectionChange={field.onChange}
                      onBlur={field.onBlur}
                      errorMessage={fieldState.error ? fieldState.error.message : undefined}
                      validationState={fieldState.error ? 'invalid' : 'valid'}
                      aria-label={`Input '${input.name}'`}
                    >
                      {Object.entries(input.options).map(([label, val]) => (
                        <Item key={val?.toString()}>{label}</Item>
                      ))}
                    </Picker>
                  )}
                </View>
              );
            } else if (isMultiSelectInput(input)) {
              const display = input.display === 'AUTO' ? (Object.entries(input.options).length <= 3 ? 'CHECKBOX' : 'LIST') : input.display;

              return display === 'CHECKBOX' ? (
                <CheckboxGroup
                  value={field.value ?? []}
                  onChange={field.onChange}
                  onBlur={field.onBlur}
                  isRequired={input.required}
                  orientation="horizontal"
                  label={label}
                  description={description}
                  errorMessage={fieldState.error ? fieldState.error.message : undefined}
                  isInvalid={!!fieldState.error}
                  aria-label={`Input '${input.name}'`}
                >
                  {Object.entries(input.options).map(([label, val]) => (
                    <Checkbox key={val?.toString()} value={val?.toString() || ''}>
                      {label}
                    </Checkbox>
                  ))}
                </CheckboxGroup>
              ) : (
                <Field label={label} description={description} width="100%" errorMessage={fieldState.error ? fieldState.error.message : undefined} validationState={fieldState.error ? 'invalid' : 'valid'}>
                  <div>
                    <ListView maxHeight="size-1600" selectionMode="multiple" selectedKeys={field.value as string[]} onSelectionChange={(val) => field.onChange(Array.from(val as Set<string>))} aria-label={`Input '${input.name}'`}>
                      {Object.entries(input.options).map(([label, val]) => (
                        <Item key={val?.toString()}>{label}</Item>
                      ))}
                    </ListView>
                  </div>
                </Field>
              );
            } else if (isNumberInput(input)) {
              if (input.display === 'SLIDER') {
                return (
                  <Field description={description} width="100%" errorMessage={fieldState.error ? fieldState.error.message : undefined} validationState={fieldState.error ? 'invalid' : 'valid'}>
                    <div>
                      <Slider
                        value={typeof field.value === 'number' ? field.value : (input.value ?? 0)}
                        onChange={field.onChange}
                        label={label}
                        minValue={input.min !== null ? input.min : 0}
                        maxValue={input.max !== null ? input.max : 100}
                        step={input.step ? input.step : 1}
                        aria-label={`Input '${input.name}'`}
                      />
                    </div>
                  </Field>
                );
              } else {
                return (
                  <NumberField
                    {...field}
                    label={label}
                    {...(fieldState.error && {
                      validationState: 'invalid',
                      errorMessage: fieldState.error.message,
                    })}
                    isRequired={input.required}
                    description={description}
                    minValue={input.min !== null ? input.min : undefined}
                    maxValue={input.max !== null ? input.max : undefined}
                    hideStepper={input.type === 'DECIMAL'}
                    formatOptions={input.type === 'INTEGER' ? { maximumFractionDigits: 0 } : undefined}
                    aria-label={`Input '${input.name}'`}
                  />
                );
              }
            } else if (isColorInput(input)) {
              return (
                <Field description={description} width="100%" errorMessage={fieldState.error ? fieldState.error.message : undefined} validationState={fieldState.error ? 'invalid' : 'valid'}>
                  <div>
                    <ColorPicker label={label} aria-label={`Input '${input.name}'`} value={field.value ? parseColor(field.value) : undefined} onChange={(value) => field.onChange(value.toString(input.format.toLowerCase() as ColorFormat))}>
                      <ColorEditor hideAlphaChannel={input.format !== 'RGBA'} />
                      <Button onPress={() => field.onChange('')} width={'100%'} marginTop={'size-200'} variant={'secondary'}>
                        Clear
                      </Button>
                    </ColorPicker>
                  </div>
                </Field>
              );
            } else if (isRangeInput(input)) {
              return (
                <Field description={description} width="100%" errorMessage={fieldState.error ? fieldState.error.message : undefined} validationState={fieldState.error ? 'invalid' : 'valid'}>
                  <div>
                    <RangeSlider value={field.value ?? { start: 0, end: 0 }} onChange={field.onChange} minValue={input.min ?? 0} maxValue={input.max ?? 100} step={input.step ?? 1} label={label} aria-label={`Input '${input.name}'`} />
                  </div>
                </Field>
              );
            } else if (isPathInput(input)) {
              return (
                <PathField
                  value={field.value ?? ''}
                  onSelect={field.onChange}
                  description={description}
                  label={label}
                  isRequired={input.required}
                  root={input.rootPath}
                  errorMessage={fieldState.error ? fieldState.error.message : undefined}
                  validationState={fieldState.error ? 'invalid' : 'valid'}
                />
              );
            } else if (isFileInput(input)) {
              return (
                <Field label={label} description={description} width="100%" errorMessage={fieldState.error ? fieldState.error.message : undefined} validationState={fieldState.error ? 'invalid' : 'valid'}>
                  <div>
                    <FileUploader allowMultiple={false} mimeTypes={input.mimeTypes} value={field.value ?? ''} onChange={field.onChange} />
                  </div>
                </Field>
              );
            } else if (isMultiFileInput(input)) {
              return (
                <Field label={label} description={description} width="100%" errorMessage={fieldState.error ? fieldState.error.message : undefined} validationState={fieldState.error ? 'invalid' : 'valid'}>
                  <div>
                    <FileUploader allowMultiple={true} min={input.min ? input.min : undefined} max={input.max ? input.max : undefined} mimeTypes={input.mimeTypes} value={field.value ?? ''} onChange={field.onChange} />
                  </div>
                </Field>
              );
            } else if (isMapInput(input)) {
              const items = Object.entries(field.value ?? {}).map(([key, value]) => ({ key, value: value?.toString() ?? '' }));
              const handleChange = (items: { key: string; value: string }[]) => {
                const record = Object.fromEntries(items.map(({ key, value }) => [key, value]));
                field.onChange(record);
              };
              return (
                <Field label={label} description={description} width="100%" errorMessage={fieldState.error ? fieldState.error.message : undefined} validationState={fieldState.error ? 'invalid' : 'valid'}>
                  <div>
                    <KeyValueEditor items={items} onChange={handleChange} uniqueKeys keyLabel={input.keyLabel} valueLabel={input.valueLabel} />
                  </div>
                </Field>
              );
            } else if (isKeyValueListInput(input)) {
              return (
                <Field label={label} description={description} width="100%" errorMessage={fieldState.error ? fieldState.error.message : undefined} validationState={fieldState.error ? 'invalid' : 'valid'}>
                  <div>
                    <KeyValueEditor items={field.value ?? []} onChange={field.onChange} keyLabel={input.keyLabel} valueLabel={input.valueLabel} />
                  </div>
                </Field>
              );
            } else {
              throw new Error(`Unsupported input type: ${input.type}`);
            }
          })()}
        </View>
      )}
    />
  );
};

export default CodeInput;
