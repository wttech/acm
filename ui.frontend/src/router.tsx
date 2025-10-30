import { createHashRouter, Navigate } from 'react-router-dom';
import App from './App';
import ErrorHandler from './ErrorHandler';
import { Route } from './Route';
import ConsolePage from './pages/ConsolePage';
import DashboardPage from './pages/DashboardPage';
import ExecutionView from './pages/ExecutionView';
import HistoryPage from './pages/HistoryPage';
import MaintenancePage from './pages/MaintenancePage';
import ScriptsPage from './pages/ScriptsPage';
import ScriptView from './pages/ScriptView';
import SnippetsPage from './pages/SnippetsPage';

const router = createHashRouter([
  {
    path: '/',
    element: <App />,
    errorElement: <ErrorHandler />,
    children: [
      { path: '/', element: <DashboardPage /> },
      { path: '/scripts/:tab?', element: <ScriptsPage /> },
      { path: '/scripts/view/:scriptId', element: <ScriptView /> },
      { path: '/snippets/:tab?', element: <SnippetsPage /> },
      { path: '/console', element: <Route permission="console"><ConsolePage /></Route> },
      { path: '/history', element: <HistoryPage /> },
      { path: '/executions', element: <HistoryPage /> },
      { path: '/executions/view/:executionId/:tab?', element: <ExecutionView /> },
      { path: '/maintenance/:tab?', element: <MaintenancePage /> },
      { path: '*', element: <Navigate to="/" replace /> },
    ],
  },
]);

export default router;
