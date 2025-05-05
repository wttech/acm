import { useAsync } from 'react-use';
import { apiRequest } from '../utils/api';
import { isExecutableScript, ScriptOutput, ScriptStats } from '../utils/api.types';

export const useScriptStats = (id: string | null) => {
  const state = useAsync(async () => {
    if (!id || !isExecutableScript(id)) {
      return null;
    }
    const response = await apiRequest<ScriptOutput>({
      operation: 'Fetch script stats',
      url: `/apps/acm/api/script.json?id=${encodeURIComponent(id)}`,
      method: 'get',
    });
    return response.data.data.stats.find((stat: ScriptStats) => stat.path === id) || null;
  }, [id]);

  return state.value;
};
