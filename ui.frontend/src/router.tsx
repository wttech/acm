import { createHashRouter } from 'react-router-dom';
import App from './App';
import ConsolePage from './pages/ConsolePage';
import ScriptsPage from './pages/ScriptsPage';
import ExecutionList from './pages/ExecutionList';
import ExecutionView from './pages/ExecutionView';
import SnippetsPage from './pages/SnippetsPage';
import ScriptView from './pages/ScriptView';
import MaintenancePage from './pages/MaintenancePage.tsx';
import HomePage from "./pages/HomePage.tsx";

const router = createHashRouter([
    {
        path: '/',
        element: <App />,
        children: [
            { path: '/', element: <HomePage/> },
            { path: '/scripts/:tab?', element: <ScriptsPage /> },
            { path: '/scripts/view/:scriptId', element: <ScriptView /> },
            { path: '/snippets/:tab?', element: <SnippetsPage /> },
            { path: '/console', element: <ConsolePage /> },
            { path: '/executions', element: <ExecutionList /> },
            { path: '/executions/view/:executionId', element: <ExecutionView /> },
            { path: '/maintenance/:tab?', element: <MaintenancePage /> },
        ],
    },
]);

export default router;