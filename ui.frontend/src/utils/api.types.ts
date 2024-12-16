export type DataExecution = {
    id: string;
    output: string;
    error: string;
    status: string;
}

export type DataAssistCode = {
    code: string;
    suggestions: {
        k: string // kind
        l: string // label
        it: string // insert text
        i: string // info
    }[]
}

export type DataSnippet = {
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

export type DataScript = {
    list: Script[];
}

export type Execution = {
    executable: Executable;
    status: string;
    duration: number;
    error: string;
    output: string;
}

export type Executable = {
    id: string;
    content: string;
}
