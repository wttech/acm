import axios from "axios";
import * as monaco from 'monaco-editor';

// Integrate with AEM's CSRF Protection
// See: https://experienceleague.adobe.com/en/docs/experience-manager-learn/cloud-service/developing/advanced/csrf-protection#fetch-with-csrf-protection
async function getCsrfToken() {
    const response = await axios.get('/libs/granite/csrf/token.json');
    return response.data.token;
}

axios.interceptors.request.use(async (config) => {
    if (config.method === 'post') {
        const csrfToken = await getCsrfToken();
        config.headers['CSRF-Token'] = csrfToken;
    }
    return config;
}, (error) => {
    return Promise.reject(error);
});

// Initialize Monaco Editor to be using embedded resources (to avoid CORS/CSP issues)
// loader.config({
//     paths: {vs: process.env.NODE_ENV === 'production' ? '/apps/migrator/spa/js/monaco-editor/vs' : '/node_modules/monaco-editor/min/vs'},
// });

export const groovyLanguage = {
    id: 'groovy',
    extensions: ['.groovy'],
    aliases: ['Groovy', 'groovy'],
    mimetypes: ['text/x-groovy'],
    loader: () => {
        return new Promise((resolve) => {
            resolve({
                tokenizer: {
                    root: [
                        [/\b(def|class|if|else|for|while|return)\b/, 'keyword'],
                        [/[a-z_$][\w$]*/, 'identifier'],
                        [/\d+/, 'number'],
                        [/".*?"/, 'string'],
                        [/\/\/.*$/, 'comment'],
                    ]
                }
            });
        });
    },
};

monaco.languages.register({ id: 'groovy' });
monaco.languages.setMonarchTokensProvider('groovy', groovyLanguage.loader() as monaco.Thenable<monaco.languages.IMonarchLanguage>);

const container = document.getElementById('container');
if (container) {
    monaco.editor.create(container, {
        value: `def hello() {
    // This is a comment
    println "Hello, World!"
    return 42
}`,
        language: 'groovy',
        theme: 'vs-dark'
    });
} else {
    console.error('Container element not found');
}
