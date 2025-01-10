import { Route, Navigate } from 'react-router-dom';

import ScriptsPage from '../pages/ScriptsPage';
import ConsolePage from '../pages/ConsolePage';

import { Routes } from 'react-router-dom';
import ExecutionList from '../pages/ExecutionList.tsx';
import SnippetsPage from '../pages/SnippetsPage.tsx';
import ExecutionView from '../pages/ExecutionView.tsx';

const Content = () => {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/console" />} />
      <Route path="/scripts" element={<ScriptsPage />} />
      <Route path="/snippets" element={<SnippetsPage />} />
      <Route path="/console" element={<ConsolePage />} />
      <Route path="/executions" element={<ExecutionList />} />
      <Route path="/executions/view/:executionId" element={<ExecutionView />} />
    </Routes>
  );
};

export default Content;
