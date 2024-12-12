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

export type DataScript = {
    list: Script[]
}

export type Script = {
    id: string
    content: string
}
