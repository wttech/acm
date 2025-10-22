import { KeyValue, MapValue, RangeValue } from './generic';

export type InputType = 'BOOL' | 'STRING' | 'TEXT' | 'SELECT' | 'MULTISELECT' | 'INTEGER' | 'DECIMAL' | 'DATETIME' | 'DATE' | 'TIME' | 'COLOR' | 'NUMBER_RANGE' | 'PATH' | 'FILE' | 'MULTIFILE' | 'MAP' | 'KEY_VALUE_LIST';
export type InputValue = string | string[] | number | number[] | boolean | null | undefined | RangeValue | KeyValue | MapValue;
export type InputValues = Record<string, InputValue>;

export const InputGroupDefault = 'general';

export type Input<T> = {
  name: string;
  type: InputType;
  value: T;
  label: string;
  description?: string;
  required: boolean;
  group: string;
  validator?: string;
};
export type MinMaxInput = Input<InputValue> & {
  min: number;
  max: number;
};

export type BoolInput = Input<boolean> & {
  display: 'SWITCHER' | 'CHECKBOX';
};

export type DateTimeInput = Input<string> & {
  min: string;
  max: string;
};

export type TextInput = Input<string> & {
  language?: string;
};

export type StringInput = Input<string> & {
  display: 'TEXT' | 'PASSWORD' | 'URL' | 'TEL' | 'EMAIL' | 'NUMERIC' | 'DECIMAL';
};

export type NumberInput = Input<number> &
  MinMaxInput & {
    step: number;
    display: 'INPUT' | 'SLIDER';
  };

export type ColorInput = Input<string> & {
  format: 'HEX' | 'RGBA' | 'HSL' | 'HSB';
};

export type NumberRangeInput = Input<RangeValue> &
  MinMaxInput & {
    step: number;
  };

export type SelectInput = Input<InputValue> & {
  options: Record<string, InputValue>;
  display: 'AUTO' | 'DROPDOWN' | 'RADIO';
};

export type MultiSelectInput = Input<InputValue> & {
  options: Record<string, InputValue>;
  display: 'AUTO' | 'CHECKBOX' | 'DROPDOWN';
};

export type PathInput = Input<InputValue> & {
  rootPath: string;
  rootInclusive: boolean;
};

export type FileInput = Input<InputValue> & {
  mimeTypes: string[];
};

export type MultiFileInput = Input<InputValue> &
  MinMaxInput & {
    mimeTypes: string[];
  };
type KeyValueBaseInput = {
  keyLabel: string;
  valueLabel: string;
};

export type MapInput = Input<MapValue> & KeyValueBaseInput & {};

export type KeyValueListInput = Input<KeyValue> & KeyValueBaseInput & {};

export function isStringInput(input: Input<InputValue>): input is StringInput {
  return input.type === 'STRING';
}

export function isBoolInput(input: Input<InputValue>): input is BoolInput {
  return input.type === 'BOOL';
}

export function isDateTimeInput(input: Input<InputValue>): input is DateTimeInput {
  return input.type === 'DATETIME' || input.type === 'DATE' || input.type === 'TIME';
}

export function isTextInput(arg: Input<InputValue>): arg is TextInput {
  return arg.type === 'TEXT';
}

export function isSelectInput(input: Input<InputValue>): input is SelectInput {
  return input.type === 'SELECT';
}

export function isMultiSelectInput(input: Input<InputValue>): input is MultiSelectInput {
  return input.type === 'MULTISELECT';
}

export function isNumberInput(input: Input<InputValue>): input is NumberInput {
  return input.type === 'INTEGER' || input.type === 'DECIMAL';
}

export function isColorInput(input: Input<InputValue>): input is ColorInput {
  return input.type === 'COLOR';
}

export function isRangeInput(input: Input<InputValue>): input is NumberRangeInput {
  return input.type === 'NUMBER_RANGE';
}

export function isPathInput(input: Input<InputValue>): input is PathInput {
  return input.type === 'PATH';
}

export function isFileInput(input: Input<InputValue>): input is FileInput {
  return input.type === 'FILE';
}

export function isMultiFileInput(input: Input<InputValue>): input is MultiFileInput {
  return input.type === 'MULTIFILE';
}

export function isMapInput(input: Input<InputValue>): input is MapInput {
  return input.type === 'MAP';
}

export function isKeyValueListInput(input: Input<InputValue>): input is KeyValueListInput {
  return input.type === 'KEY_VALUE_LIST';
}

export function stringInputDisplayToType(display: string): string {
  switch (display) {
    case 'PASSWORD':
      return 'password';
    case 'URL':
      return 'url';
    case 'TEL':
      return 'tel';
    case 'EMAIL':
      return 'email';
    case 'TEXT':
    default:
      return 'text';
  }
}

export function stringInputDisplayToMode(display: string): "url" | "tel" | "email" | "text" | "numeric" | "decimal" {
  switch (display) {
    case 'URL':
      return 'url';
    case 'TEL':
      return 'tel';
    case 'EMAIL':
      return 'email';
    case 'NUMERIC':
      return 'numeric';
    case 'DECIMAL':
      return 'decimal';
    case 'TEXT':
    case 'PASSWORD':
    default:
      return 'text';
  }
}
