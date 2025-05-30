import { useFormContext } from 'react-hook-form';
import { Argument, ArgumentValue, isDateTimeArgument, isNumberArgument, isPathArgument } from '../utils/api.types.ts';
import { Dates } from '../utils/dates';

type ValidationResult = string | true | undefined;

export const useArgumentInput = (arg: Argument<ArgumentValue>) => {
  const { control, getValues } = useFormContext();

  const controllerRules = {
    validate: (value: ArgumentValue): ValidationResult => {
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

function isValueEmpty(value: ArgumentValue): boolean {
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

function isValueAvailable(value: ArgumentValue): boolean {
  return !isValueEmpty(value);
}

function validateCustom(arg: Argument<ArgumentValue>, value: ArgumentValue, allValues: Record<string, ArgumentValue>): ValidationResult {
  try {
    const validator = eval(arg.validator!);
    const errorMessage = validator(value, allValues);
    return errorMessage || true;
  } catch (error) {
    console.error(`Validator for argument '${arg.name}' failed!`, error);
    return `Validator failed`;
  }
}

function validateDefault(arg: Argument<ArgumentValue>, value: ArgumentValue): ValidationResult {
  if (isNumberArgument(arg) && typeof value === 'number') {
    if (arg.min && value < arg.min) {
      return `Value must be greater than or equal to '${arg.min}'`;
    }
    if (arg.max && value > arg.max) {
      return `Value must be less than or equal to '${arg.max}'`;
    }
    if (arg.step && (value - (arg.min || 0)) % arg.step !== 0) {
      return `Value must be a multiple of '${arg.step}'`;
    }
  } else if (isPathArgument(arg) && typeof value === 'string') {
    if (!value.startsWith('/')) {
      return `Path must start with '/'`;
    } else if (arg.root && !value.startsWith(`${arg.root}/`)) {
      return `Path must start with '${arg.root}/'`;
    }
  } else if (isDateTimeArgument(arg) && typeof value === 'string') {
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
