/** Narrows an `unknown` value to a plain object; `what` names it in the error, e.g. "the document". */
export function asObject(value: unknown, what: string): Record<string, unknown> {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) {
    throw new Error(`${what} must be an object`);
  }
  return value as Record<string, unknown>;
}

/** Reads a trimmed string field; `undefined` passes only when not required. */
export function asText(value: unknown, field: string, required = true): string | undefined {
  if (value === undefined && !required) {
    return undefined;
  }
  if (typeof value !== 'string' || !value.trim()) {
    throw new Error(`${field} must be a non-empty string`);
  }
  return (value as string).trim();
}

/** Reads a boolean field; `undefined` passes through. */
export function asFlag(value: unknown, field: string): boolean | undefined {
  if (value === undefined) {
    return undefined;
  }
  if (typeof value !== 'boolean') {
    throw new Error(`${field} must be a boolean`);
  }
  return value;
}

/** Reads a positive number field; `undefined` passes through. */
export function asPositiveNumber(value: unknown, field: string): number | undefined {
  if (value === undefined) {
    return undefined;
  }
  if (typeof value !== 'number' || !(value > 0)) {
    throw new Error(`${field} must be a positive number`);
  }
  return value;
}
