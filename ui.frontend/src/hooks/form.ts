import { useFormContext } from 'react-hook-form';
import { Input, InputValue, isDateTimeInput, isMultiFileInput, isNumberInput, isPathInput } from '../types/input.ts';
import { Dates } from '../utils/dates';

type ValidationResult = string | true | undefined;

export const useInput = (arg: Input<InputValue>) => {
  const { control, getValues } = useFormContext();

  const controllerRules = {
    validate: (value: InputValue): ValidationResult => {
      if (arg.required && isValueEmpty(value)) {
        return 'Value is required';
      }
      if (arg.validator) {
        return validateCustom(arg, value, getValues());
      } else if (isValueAvailable(value)) {
        return validateDefault(arg, value);
      }
      return true;
    },
  };

  return { control, controllerRules };
};

function isValueEmpty(value: InputValue): boolean {
  return (
    value === null ||
    value === undefined ||
    value === '' ||
    (typeof value === 'number' && isNaN(value)) ||
    (typeof value == 'boolean' && !value) ||
    (Array.isArray(value) && value.length === 0) ||
    (typeof value === 'object' && Object.keys(value).length === 0)
  );
}

function isValueAvailable(value: InputValue): boolean {
  return !isValueEmpty(value);
}

function validateCustom(arg: Input<InputValue>, value: InputValue, allValues: Record<string, InputValue>): ValidationResult {
  try {
    const validator = eval(arg.validator!);
    const errorMessage = validator(value, allValues);
    return errorMessage || true;
  } catch (error) {
    console.error(`Validator for input '${arg.name}' failed!`, error);
    return `Validator failed`;
  }
}

function validateDefault(arg: Input<InputValue>, value: InputValue): ValidationResult {
  if (isMultiFileInput(arg)) {
    const files = Array.isArray(value) ? value : value ? [value] : [];
    const min = typeof arg.min === 'number' ? arg.min : undefined;
    const max = typeof arg.max === 'number' ? arg.max : undefined;

    if ((min && files.length < min) || (max && files.length > max)) {
      let msg = 'Files count allowed:';
      if (min) {
        msg += ` minimum ${min}`;
      }
      if (max) {
        msg += `, maximum ${max}`;
      }
      return msg;
    }
  } else if (isNumberInput(arg) && typeof value === 'number') {
    if (arg.min && value < arg.min) {
      return `Value must be greater than or equal to '${arg.min}'`;
    }
    if (arg.max && value > arg.max) {
      return `Value must be less than or equal to '${arg.max}'`;
    }
    if (arg.step && (value - (arg.min || 0)) % arg.step !== 0) {
      return `Value must be a multiple of '${arg.step}'`;
    }
  } else if (isPathInput(arg) && typeof value === 'string') {
    if (!value.startsWith('/')) {
      return `Path must start with '/'`;
    } else if (value.includes('//')) {
      return `Path must not contain consecutive slashes '//'`;
    } else if (value.endsWith(' ')) {
      return `Path must not end with a space`;
    } else if (arg.rootPath) {
      if (value !== '/' && value.endsWith('/')) {
        return `Path must not end with '/'`;
      } else if (arg.rootInclusive && !value.startsWith(arg.rootPath)) {
        return `Path must start with '${arg.rootPath}'`;
      } else if (!arg.rootInclusive) {
        if (arg.rootPath === '/') {
          if (!value.startsWith('/')) {
            return `Path must start with '/'`;
          }
        } else if (!value.startsWith(`${arg.rootPath}/`)) {
          return `Path must start with '${arg.rootPath}/'`;
        }
      }
    }
  } else if (isDateTimeInput(arg) && typeof value === 'string') {
    if (arg.type === 'DATE') {
      if (arg.min && Dates.toCalendarDate(value) < Dates.toCalendarDate(arg.min)) {
        return `Value must be greater than or equal to '${arg.min}'`;
      }
      if (arg.max && Dates.toCalendarDate(value) > Dates.toCalendarDate(arg.max)) {
        return `Value must be less than or equal to '${arg.max}'`;
      }
    } else if (arg.type === 'TIME') {
      if (arg.min && Dates.toTime(value) < Dates.toTime(arg.min)) {
        return `Value must be greater than or equal to '${arg.min}'`;
      }
      if (arg.max && Dates.toTime(value) > Dates.toTime(arg.max)) {
        return `Value must be less than or equal to '${arg.max}'`;
      }
    } else if (arg.type === 'DATETIME') {
      if (arg.min && Dates.toCalendarDateTime(value) < Dates.toCalendarDateTime(arg.min)) {
        return `Value must be greater than or equal to '${arg.min}'`;
      }
      if (arg.max && Dates.toCalendarDateTime(value) > Dates.toCalendarDateTime(arg.max)) {
        return `Value must be less than or equal to '${arg.max}'`;
      }
    }
  }
}
