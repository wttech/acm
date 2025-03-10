import { Navigate, Route } from 'react-router-dom';
import ConsolePage from '../pages/ConsolePage';
import ScriptsPage from '../pages/ScriptsPage';
import { Routes } from 'react-router-dom';
import ExecutionList from '../pages/ExecutionList.tsx';
import ExecutionView from '../pages/ExecutionView.tsx';
import SnippetsPage from '../pages/SnippetsPage.tsx';
import ScriptView from "../pages/ScriptView.tsx";
import MaintenancePage from "../pages/MaintenancePage.tsx";

const Content = () => {
    return (
        <Routes>
            <Route path="/" element={<Navigate to="/console" />} />
            <Route path="/scripts/:tab?" element={<ScriptsPage />} />
            <Route path="/scripts/view/:scriptId" element={<ScriptView />} />
            <Route path="/snippets/:tab?" element={<SnippetsPage />} />
            <Route path="/console" element={<ConsolePage />} />
            <Route path="/executions" element={<ExecutionList />} />
            <Route path="/executions/view/:executionId" element={<ExecutionView />} />
            <Route path="/settings/:tab?" element={<MaintenancePage />} />
        </Routes>
    );
};

export default Content;