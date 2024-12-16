export type Executable = {
    id: string;
    content: string;
}

export type Execution = {
    id: string;
    executable: Executable;
    status: ExecutionStatus;
    startDate: string;
    endDate: string;
    duration: number;
    output: string;
    error: string;
}

export type ExecutionStatus = 'QUEUED' | 'ACTIVE' | 'STOPPED' | 'FAILED' | 'SKIPPED' | 'ABORTED' | 'SUCCEEDED';

export type ExecutionOutput = {
    list: Execution[];
}

export type AssistCodeOutput = {
    code: string;
    suggestions: {
        k: string // kind
        l: string // label
        it: string // insert text
        i: string // info
    }[]
}

export type SnippetOutput = {
    list: Snippet[]
}

export type Snippet = {
    name: string
    id: string
    content: string
    documentation: string
}

export type Script = {
    id: string;
    path: string;
    name: string;
    content: string;
}

export type ScriptOutput = {
    list: Script[];
}
