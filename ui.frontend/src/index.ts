import { loader as monacoLoader } from '@monaco-editor/react';
import axios from 'axios';
import mermaid from 'mermaid';
import { devServerPort, isProduction } from './utils/node.ts';

// Integrate with AEM's CSRF Protection
// See: https://experienceleague.adobe.com/en/docs/experience-manager-learn/cloud-service/developing/advanced/csrf-protection#fetch-with-csrf-protection
async function getCsrfToken() {
  const response = await axios.get('/libs/granite/csrf/token.json');
  return response.data.token;
}

axios.interceptors.request.use(
  async (config) => {
    // Granite shell page has a different CSRF token (no need to set it for dev development)
    if (!isProduction()) {
      if (['post', 'delete', 'put'].includes(config.method || '')) {
        const csrfToken = await getCsrfToken();
        config.headers['CSRF-Token'] = csrfToken;
      }
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  },
);

// Initialize Monaco Editor to be using embedded resources (to avoid CORS/CSP issues)
monacoLoader.config({
  paths: {
    vs: isProduction() ? `${window.origin}/apps/acm/gui/spa/build/js/monaco-editor/vs` : `http://localhost:${devServerPort}/acm/js/monaco-editor/vs`,
  },
});

// Initialize Mermaid for diagram rendering in Markdown component
mermaid.initialize({
  startOnLoad: false,
  theme: 'neutral',
  securityLevel: 'loose',
});
