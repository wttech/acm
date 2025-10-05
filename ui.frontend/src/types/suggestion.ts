export enum SuggestionKind {
  VARIABLE = 'variable',
  RESOURCE = 'resource',
  SNIPPET = 'snippet',
  CLASS = 'class',
}

export type Suggestion = {
  k: SuggestionKind; // kind
  l: string; // label
  it: string; // insert text
  i: string; // info
};
