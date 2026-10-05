import type { CatalogEntry } from './lifecycle';

// Mirrors core/.../code/Inputs.java.
// TODO: add options of each *Input class (label, value, validator, ...) for completion inside closures.
const INPUT_TYPES = [
  'bool',
  'string',
  'text',
  'select',
  'multiSelect',
  'integerNumber',
  'decimalNumber',
  'integerRange',
  'decimalRange',
  'date',
  'time',
  'dateTime',
  'color',
  'path',
  'file',
  'multiFile',
  'map',
  'keyValueList',
] as const;

export type InputType = (typeof INPUT_TYPES)[number];

export const INPUT_METHODS: CatalogEntry[] = INPUT_TYPES.map((type) => ({
  name: type,
  signature: `inputs.${type}(String name, Closure options = null)`,
  docs: `Declares a \`${type}\` input.`,
  snippet: `${type}('\${1:name}')`,
}));
