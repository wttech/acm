import api from './api.json';

export interface ApiParam {
  type: string;
  name: string;
}

export interface ApiMethod {
  name: string;
  returns: string;
  params: ApiParam[];
  deprecated?: boolean;
  doc?: string;
}

export interface ApiClass {
  name: string;
  superclass?: string;
  values?: string[];
  methods: ApiMethod[];
}

export interface ApiLifecycleMethod {
  name: string;
  returns: string;
  required: boolean;
  paramCount: number;
}

export interface ApiBinding {
  name: string;
  type: string;
  class: string | null;
  deprecated?: boolean;
}

/** Script DSL method taking a configuration closure, e.g. `inputs.string('name') { ... }`. */
export interface ApiDsl {
  name: string;
  returns: string;
  options: string;
  optionsRequired: boolean;
  deprecated?: boolean;
}

export interface ScriptApi {
  lifecycle: { content: ApiLifecycleMethod[]; extension: ApiLifecycleMethod[]; mock: ApiLifecycleMethod[] };
  bindings: ApiBinding[];
  inputs: ApiDsl[];
  outputs: ApiDsl[];
  classes: Record<string, ApiClass>;
}

/** ACM Groovy script API generated from core/ Java sources by tools/shared/codegen.mjs. */
export const SCRIPT_API: ScriptApi = api;

export function methodSignature(method: ApiMethod): string {
  return `${method.returns} ${method.name}(${method.params.map((p) => `${p.type} ${p.name}`).join(', ')})`;
}
