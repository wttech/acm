import { createHashRouter } from 'react-router-dom';
import App from './App';
import ConsolePage from './pages/ConsolePage';
import ScriptsPage from './pages/ScriptsPage';
import HistoryPage from './pages/HistoryPage.tsx';
import ExecutionView from './pages/ExecutionView';
import SnippetsPage from './pages/SnippetsPage';
import ScriptView from './pages/ScriptView';
import MaintenancePage from './pages/MaintenancePage.tsx';
import DashboardPage from "./pages/DashboardPage.tsx";

const router = createHashRouter([
    {
        path: '/',
        element: <App />,
        children: [
            { path: '/', element: <DashboardPage/> },
            { path: '/scripts/:tab?', element: <ScriptsPage /> },
            { path: '/scripts/view/:scriptId', element: <ScriptView /> },
            { path: '/snippets/:tab?', element: <SnippetsPage /> },
            { path: '/console', element: <ConsolePage /> },
            { path: '/history', element: <HistoryPage /> },
            { path: '/executions', element: <HistoryPage /> },
            { path: '/executions/view/:executionId', element: <ExecutionView /> },
            { path: '/maintenance/:tab?', element: <MaintenancePage /> },
        ],
    },
]);

export default router;