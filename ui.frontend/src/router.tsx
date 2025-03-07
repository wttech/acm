import { createHashRouter } from 'react-router-dom';
import App from './App';
import ConsolePage from './pages/ConsolePage';
import ScriptsPage from './pages/ScriptsPage';
import ExecutionList from './pages/ExecutionList';
import ExecutionView from './pages/ExecutionView';
import SnippetsPage from './pages/SnippetsPage';
import ScriptView from './pages/ScriptView';
import SettingsPage from './pages/SettingsPage';
import { Navigate } from 'react-router-dom';

const router = createHashRouter([
    {
        path: '/',
        element: <App />,
        children: [
            { path: '/', element: <Navigate to="/console" /> },
            { path: '/scripts/:tab?', element: <ScriptsPage /> },
            { path: '/scripts/view/:scriptId', element: <ScriptView /> },
            { path: '/snippets/:tab?', element: <SnippetsPage /> },
            { path: '/console', element: <ConsolePage /> },
            { path: '/executions', element: <ExecutionList /> },
            { path: '/executions/view/:executionId', element: <ExecutionView /> },
            { path: '/settings/:tab?', element: <SettingsPage /> },
        ],
    },
]);

export default router;