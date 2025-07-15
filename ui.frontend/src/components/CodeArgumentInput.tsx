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
import { Editor } from '@monaco-editor/react';
import { Field } from '@react-spectrum/label';
import React from 'react';
import { Controller } from 'react-hook-form';
import { useArgumentInput } from '../hooks/form';
import {
  Argument,
  ArgumentValue,
  isBoolArgument,
  isColorArgument,
  isDateTimeArgument,
  isFileArgument,
  isKeyValueListArgument,
  isMapArgument,
  isMultiFileArgument,
  isMultiSelectArgument,
  isNumberArgument,
  isPathArgument,
  isRangeArgument,
  isSelectArgument,
  isStringArgument,
  isTextArgument,
} from '../utils/api.types.ts';
import { Dates } from '../utils/dates';
import { Strings } from '../utils/strings';
import FileUploader from './FileUploader';
import Markdown from './Markdown';
import PathField from './PathPicker';

interface CodeArgumentInputProps {
  arg: Argument<ArgumentValue>;
  value: ArgumentValue;
  onChange: (name: string, value: ArgumentValue) => void;
}

const CodeArgumentInput: React.FC<CodeArgumentInputProps> = ({ arg }) => {
  const { control, controllerRules } = useArgumentInput(arg);
  const label = arg.label || Strings.capitalizeWords(arg.name);
  const description = arg.description ? <Markdown>{arg.description}</Markdown> : undefined;

  return (
    <Controller
      name={arg.name}
      control={control}
      rules={controllerRules}
      render={({ field, fieldState }) => (
        <View key={arg.name} marginBottom="size-200">
          {(() => {
            if (isBoolArgument(arg)) {
              return (
                <Field description={description} width="100%" errorMessage={fieldState.error ? fieldState.error.message : undefined} validationState={fieldState.error ? 'invalid' : 'valid'}>
                  <div>
                    {arg.display === 'SWITCHER' ? (
                      <Switch {...field} isSelected={field.value} onChange={field.onChange} aria-label={`Argument '${arg.name}'`}>
                        {label}
                      </Switch>
                    ) : (
                      <Checkbox {...field} isSelected={field.value} isInvalid={!!fieldState.error} onChange={field.onChange} aria-label={`Argument '${arg.name}'`}>
                        {label}
                      </Checkbox>
                    )}
                  </div>
                </Field>
              );
            } else if (isDateTimeArgument(arg)) {
              return arg.type === 'DATE' ? (
                <DateField
                  {...field}
                  value={Dates.toCalendarDateOrNull(field.value)}
                  minValue={arg.min !== null ? Dates.toCalendarDate(arg.min) : undefined}
                  maxValue={arg.max !== null ? Dates.toCalendarDate(arg.max) : undefined}
                  onChange={(dateValue) => field.onChange(dateValue?.toString())}
                  label={label}
                  {...(fieldState.error && {
                    validationState: 'invalid',
                    errorMessage: fieldState.error.message,
                  })}
                  description={description}
                  aria-label={`Argument '${arg.name}'`}
                />
              ) : arg.type === 'TIME' ? (
                <TimeField
                  {...field}
                  value={Dates.toTimeOrNull(field.value)}
                  minValue={arg.min !== null ? Dates.toTime(arg.min) : undefined}
                  maxValue={arg.max !== null ? Dates.toTime(arg.max) : undefined}
                  onChange={(timeValue) => field.onChange(timeValue?.toString())}
                  label={label}
                  {...(fieldState.error && {
                    validationState: 'invalid',
                    errorMessage: fieldState.error.message,
                  })}
                  description={description}
                  aria-label={`Argument '${arg.name}'`}
                />
              ) : (
                <DatePicker
                  {...field}
                  value={Dates.toCalendarDateTimeOrNull(field.value)}
                  minValue={arg.min !== null ? Dates.toCalendarDateTime(arg.min) : undefined}
                  maxValue={arg.max !== null ? Dates.toCalendarDateTime(arg.max) : undefined}
                  onChange={(dateValue) => field.onChange(dateValue?.toString())}
                  granularity="second"
                  label={label}
                  {...(fieldState.error && {
                    validationState: 'invalid',
                    errorMessage: fieldState.error.message,
                  })}
                  description={description}
                  aria-label={`Argument '${arg.name}'`}
                />
              );
            } else if (isStringArgument(arg)) {
              return (
                <TextField
                  type={arg.display}
                  {...field}
                  value={field.value ?? ''}
                  label={label}
                  description={description}
                  errorMessage={fieldState.error ? fieldState.error.message : undefined}
                  {...(fieldState.error && { validationState: 'invalid' })}
                  isRequired={arg.required}
                  aria-label={`Argument '${arg.name}'`}
                  width={arg.display !== 'PASSWORD' ? '100%' : undefined}
                />
              );
            } else if (isTextArgument(arg)) {
              return arg.language ? (
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
                    <View width="100%" backgroundColor="gray-800" borderWidth="thin" position="relative" borderColor="dark" height="100%" borderRadius="medium" padding="size-50">
                      <Editor aria-label={`Argument '${arg.name}'`} language={arg.language} theme="vs-dark" height="200px" options={{ scrollBeyondLastLine: false }} value={field.value?.toString() || ''} onChange={field.onChange} />
                    </View>
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
                  aria-label={`Argument '${arg.name}'`}
                  width="100%"
                />
              );
            } else if (isSelectArgument(arg)) {
              const display = arg.display === 'AUTO' ? (Object.entries(arg.options).length <= 3 ? 'RADIO' : 'DROPDOWN') : arg.display;
              return (
                <View key={arg.name} marginBottom="size-200">
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
                      label={label}
                      description={description}
                      selectedKey={field.value?.toString() || ''}
                      onSelectionChange={field.onChange}
                      onBlur={field.onBlur}
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
              );
            } else if (isMultiSelectArgument(arg)) {
              const display = arg.display === 'AUTO' ? (Object.entries(arg.options).length <= 3 ? 'CHECKBOX' : 'LIST') : arg.display;

              return display === 'CHECKBOX' ? (
                <CheckboxGroup
                  value={field.value ?? []}
                  onChange={field.onChange}
                  onBlur={field.onBlur}
                  isRequired={arg.required}
                  orientation="horizontal"
                  label={label}
                  description={description}
                  errorMessage={fieldState.error ? fieldState.error.message : undefined}
                  isInvalid={!!fieldState.error}
                  aria-label={`Argument '${arg.name}'`}
                >
                  {Object.entries(arg.options).map(([label, val]) => (
                    <Checkbox key={val?.toString()} value={val?.toString() || ''}>
                      {label}
                    </Checkbox>
                  ))}
                </CheckboxGroup>
              ) : (
                <Field label={label} description={description} width="100%" errorMessage={fieldState.error ? fieldState.error.message : undefined} validationState={fieldState.error ? 'invalid' : 'valid'}>
                  <div>
                    <ListView maxHeight="size-1600" selectionMode="multiple" selectedKeys={field.value as string[]} onSelectionChange={(val) => field.onChange(Array.from(val as Set<string>))} aria-label={`Argument '${arg.name}'`}>
                      {Object.entries(arg.options).map(([label, val]) => (
                        <Item key={val?.toString()}>{label}</Item>
                      ))}
                    </ListView>
                  </div>
                </Field>
              );
            } else if (isNumberArgument(arg)) {
              if (arg.display === 'SLIDER') {
                return (
                  <Field description={description} width="100%" errorMessage={fieldState.error ? fieldState.error.message : undefined} validationState={fieldState.error ? 'invalid' : 'valid'}>
                    <div>
                      <Slider
                        value={typeof field.value === 'number' ? field.value : (arg.value ?? 0)}
                        onChange={field.onChange}
                        label={label}
                        minValue={arg.min !== null ? arg.min : 0}
                        maxValue={arg.max !== null ? arg.max : 100}
                        step={arg.step ? arg.step : 1}
                        aria-label={`Argument '${arg.name}'`}
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
                    isRequired={arg.required}
                    description={description}
                    minValue={arg.min !== null ? arg.min : undefined}
                    maxValue={arg.max !== null ? arg.max : undefined}
                    hideStepper={arg.type === 'DECIMAL'}
                    formatOptions={arg.type === 'INTEGER' ? { maximumFractionDigits: 0 } : undefined}
                    aria-label={`Argument '${arg.name}'`}
                  />
                );
              }
            } else if (isColorArgument(arg)) {
              return (
                <Field description={description} width="100%" errorMessage={fieldState.error ? fieldState.error.message : undefined} validationState={fieldState.error ? 'invalid' : 'valid'}>
                  <div>
                    <ColorPicker label={label} aria-label={`Argument '${arg.name}'`} value={field.value ? parseColor(field.value) : undefined} onChange={(value) => field.onChange(value.toString(arg.format.toLowerCase() as ColorFormat))}>
                      <ColorEditor hideAlphaChannel={arg.format !== 'RGBA'} />
                      <Button onPress={() => field.onChange('')} width={'100%'} marginTop={'size-200'} variant={'secondary'}>
                        Clear
                      </Button>
                    </ColorPicker>
                  </div>
                </Field>
              );
            } else if (isRangeArgument(arg)) {
              return (
                <Field description={description} width="100%" errorMessage={fieldState.error ? fieldState.error.message : undefined} validationState={fieldState.error ? 'invalid' : 'valid'}>
                  <div>
                    <RangeSlider value={field.value ?? { start: 0, end: 0 }} onChange={field.onChange} minValue={arg.min ?? 0} maxValue={arg.max ?? 100} step={arg.step ?? 1} label={label} aria-label={`Argument '${arg.name}'`} />
                  </div>
                </Field>
              );
            } else if (isPathArgument(arg)) {
              return (
                <PathField
                  value={field.value ?? ''}
                  onSelect={field.onChange}
                  description={description}
                  label={label}
                  isRequired={arg.required}
                  root={arg.rootPath}
                  errorMessage={fieldState.error ? fieldState.error.message : undefined}
                  validationState={fieldState.error ? 'invalid' : 'valid'}
                />
              );
            } else if (isFileArgument(arg)) {
              return (
                <Field label={label} description={description} width="100%" errorMessage={fieldState.error ? fieldState.error.message : undefined} validationState={fieldState.error ? 'invalid' : 'valid'}>
                  <div>
                    <FileUploader allowMultiple={false} mimeTypes={arg.mimeTypes} value={field.value ?? ''} onChange={field.onChange} />
                  </div>
                </Field>
              );
            } else if (isMultiFileArgument(arg)) {
              return (
                <Field label={label} description={description} width="100%" errorMessage={fieldState.error ? fieldState.error.message : undefined} validationState={fieldState.error ? 'invalid' : 'valid'}>
                  <div>
                    <FileUploader allowMultiple={true} min={arg.min ? arg.min : undefined} max={arg.max ? arg.max : undefined} mimeTypes={arg.mimeTypes} value={field.value ?? ''} onChange={field.onChange} />
                  </div>
                </Field>
              );
            } else if (isMapArgument(arg)) {
              return null // TODO impl
            } else if (isKeyValueListArgument(arg)) {
              return null // TODO impl
            } else {
              throw new Error(`Unsupported argument type: ${arg.type}`);
            }
          })()}
        </View>
      )}
    />
  );
};

export default CodeArgumentInput;
