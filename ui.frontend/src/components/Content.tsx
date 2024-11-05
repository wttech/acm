import { Route, Navigate } from 'react-router-dom'

import ScriptsPage from "../pages/ScriptsPage";
import ConsolePage from "../pages/ConsolePage";

import { Routes } from 'react-router-dom';
import ExecutionsPage from "../pages/ExecutionsPage.tsx";

const Content = () => {
    return (
        <Routes>
            <Route path="/" element={<Navigate to="/console" />} />
            <Route path="/scripts" element={<ScriptsPage/>} />
            <Route path="/console" element={<ConsolePage/>} />
            <Route path="/executions" element={<ExecutionsPage/>} />
        </Routes>
    );
};

export default Content;
