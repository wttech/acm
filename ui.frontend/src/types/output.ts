
export type Outputs = {
  [key: string]: Output;
};

export type Output = {
  name: string;
  type: OutputType;
  label: string;
  description?: string;
};

export type OutputFile = Output & {
  mimeType: string;
  downloadName: string;
};

export type OutputText = Output & {
  text: string;
};

export type OutputType = 'FILE' | 'TEXT';

export const OutputNames = {
  ARCHIVE: 'acm-archive',
  CONSOLE: 'acm-console',
} as const;
