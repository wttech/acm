import { useCallback, useEffect, useState } from 'react';
import { toastRequest } from '../utils/api';
import { ScriptOutput, ScriptType } from '../utils/api.types';

export function useScripts(type: ScriptType) {
    const [scripts, setScripts] = useState<ScriptOutput | null>(null);
    const [loading, setLoading] = useState<boolean>(true);

    const loadScripts = useCallback(() => {
        setLoading(true);
        toastRequest<ScriptOutput>({
            method: 'GET',
            url: `/apps/acm/api/script.json?type=${type}`,
            operation: `Scripts loading (${type.toString().toLowerCase()})`,
            positive: false,
        })
            .then((data) => setScripts(data.data.data))
            .catch((error) => console.error(`Scripts loading (${type}) error:`, error))
            .finally(() => setLoading(false));
    }, [type]);

    useEffect(() => {
        loadScripts();
    }, [type, loadScripts]);

    return { scripts, loading };
}