import axios from "axios";
import {loader as monacoLoader} from "@monaco-editor/react";

// Integrate with AEM's CSRF Protection
// See: https://experienceleague.adobe.com/en/docs/experience-manager-learn/cloud-service/developing/advanced/csrf-protection#fetch-with-csrf-protection
async function getCsrfToken() {
    const response = await axios.get('/libs/granite/csrf/token.json');
    return response.data.token;
}
axios.interceptors.request.use(async (config) => {
    if (['post', 'delete', 'put'].includes((config.method || ''))) {
        const csrfToken = await getCsrfToken();
        config.headers['CSRF-Token'] = csrfToken;
    }
    return config;
}, (error) => {
    return Promise.reject(error);
});

// Initialize Monaco Editor to be using embedded resources (to avoid CORS/CSP issues)
monacoLoader.config({
    paths: {vs: process.env.NODE_ENV === 'production' ? '/apps/contentor/spa/js/monaco-editor/vs' : '/node_modules/monaco-editor/min/vs'},
});
