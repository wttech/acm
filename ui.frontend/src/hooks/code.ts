import { useCallback, useEffect, useState } from 'react';
import { useDebounce } from 'react-use';
import { apiRequest } from '../utils/api.ts';
import { Execution } from '../utils/api.types.ts';

const compilationDelay = 1000;

export type SyntaxError = {
    line: number;
    column: number;
    message: string
};

export const useCompilation = (code: string | undefined, onCodeChange: (code: string) => void) => {
    const [compiling, setCompiling] = useState<boolean>(false);
    const [syntaxError, setSyntaxError] = useState<SyntaxError | undefined>(undefined);
    const [compileError, setCompileError] = useState<string | undefined>(undefined);
    const [execution, setExecution] = useState<Execution | null>(null);

    const compileCode = useCallback(async () => {
        try {
            const { data } = await apiRequest<Execution>({
                operation: 'Code parsing',
                url: `/apps/acm/api/execute-code.json`,
                method: 'post',
                data: {
                    mode: 'parse',
                    code: {
                        id: 'console',
                        content: code,
                    },
                },
            });
            const queuedExecution = data.data;
            setExecution(queuedExecution);

            if (queuedExecution.error) {
                const [, lineText, columnText] = queuedExecution.error.match(/@ line (\d+), column (\d+)/) || [];

                if (!lineText || !columnText) {
                    setCompileError(queuedExecution.error);
                    return;
                }

                const line = parseInt(lineText, 10);
                const column = parseInt(columnText, 10);

                setSyntaxError({ line, column, message: queuedExecution.error });
            } else {
                setSyntaxError(undefined);
                setCompileError(undefined);
            }
        } catch {
            console.warn('Code parsing error!');
        } finally {
            setCompiling(false);
        }
    }, [code]);

    const [, cancelCompilation] = useDebounce(compileCode, compilationDelay, [code]);
    useDebounce(() => onCodeChange(code || ''), compilationDelay, [code]);

    useEffect(() => {
        setSyntaxError(undefined);
        setCompileError(undefined);
        setCompiling(true);

        return () => {
            cancelCompilation();
        };
    }, [cancelCompilation, code]);

    return [compiling, syntaxError, compileError, execution] as const;
};