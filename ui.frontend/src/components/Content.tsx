import { Navigate, Route } from 'react-router-dom';

import ConsolePage from '../pages/ConsolePage';
import ScriptsPage from '../pages/ScriptsPage';

import { Routes } from 'react-router-dom';
import ExecutionList from '../pages/ExecutionList.tsx';
import ExecutionView from '../pages/ExecutionView.tsx';
import SnippetsPage from '../pages/SnippetsPage.tsx';

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
