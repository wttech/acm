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
        element: (
          <Route featureId="dashboard">
            <DashboardPage />
          </Route>
        ),
      },
      {
        path: '/scripts/:tab?',
        element: (
          <Route featureId="scripts">
            <ScriptsPage />
          </Route>
        ),
      },
      {
        path: '/scripts/view/:scriptId',
        element: (
          <Route featureId="scripts">
            <ScriptView />
          </Route>
        ),
      },
      {
        path: '/snippets/:tab?',
        element: (
          <Route featureId="snippets">
            <SnippetsPage />
          </Route>
        ),
      },
      {
        path: '/console',
        element: (
          <Route featureId="console">
            <ConsolePage />
          </Route>
        ),
      },
      {
        path: '/history',
        element: (
          <Route featureId="history">
            <HistoryPage />
          </Route>
        ),
      },
      {
        path: '/executions',
        element: (
          <Route featureId="history">
            <HistoryPage />
          </Route>
        ),
      },
      { path: '/executions/view/:executionId/:tab?', element: <ExecutionView /> },
      {
        path: '/maintenance/:tab?',
        element: (
          <Route featureId="maintenance">
            <MaintenancePage />
          </Route>
        ),
      },
      { path: '*', element: <Navigate to="/" replace /> },
    ],
  },
]);

export default router;
