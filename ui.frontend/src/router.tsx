import { createHashRouter, Navigate } from 'react-router-dom';
import App from './App';
import ErrorHandler from './ErrorHandler';
import ConsolePage from './pages/ConsolePage';
import DashboardPage from './pages/DashboardPage';
import ExecutionView from './pages/ExecutionView';
import HistoryPage from './pages/HistoryPage';
import MaintenancePage from './pages/MaintenancePage';
import ScriptsPage from './pages/ScriptsPage';
import ScriptView from './pages/ScriptView';
import SnippetsPage from './pages/SnippetsPage';
import { Route } from './Route';

const router = createHashRouter([
  {
    path: '/',
    element: <App />,
    errorElement: <ErrorHandler />,
    children: [
      {
        path: '/',
        element: <DashboardPage />,
      },
      {
        path: '/scripts/:tab?',
        element: (
          <Route featureId="script.list">
            <ScriptsPage />
          </Route>
        ),
      },
      {
        path: '/scripts/view/:scriptId',
        element: (
          <Route featureId="script.view">
            <ScriptView />
          </Route>
        ),
      },
      {
        path: '/snippets/:tab?',
        element: (
          <Route featureId="snippet.list">
            <SnippetsPage />
          </Route>
        ),
      },
      {
        path: '/console',
        element: (
          <Route featureId="console.view">
            <ConsolePage />
          </Route>
        ),
      },
      {
        path: '/history',
        element: (
          <Route featureId="execution.list">
            <HistoryPage />
          </Route>
        ),
      },
      {
        path: '/executions',
        element: (
          <Route featureId="execution.list">
            <HistoryPage />
          </Route>
        ),
      },
      {
        path: '/executions/view/:executionId/:tab?',
        element: (
          <Route featureId="execution.view">
            <ExecutionView />
          </Route>
        ),
      },
      {
        path: '/maintenance/:tab?',
        element: (
          <Route featureId="maintenance.view">
            <MaintenancePage />
          </Route>
        ),
      },
      { path: '*', element: <Navigate to="/" replace /> },
    ],
  },
]);

export default router;
