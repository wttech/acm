import { Route, Navigate } from 'react-router-dom'

import ScriptsPage from "./pages/ScriptsPage.tsx";
import ConsolePage from "./pages/ConsolePage.tsx";

import { Routes } from 'react-router-dom';

const AppRoutes = () => {
    return (
        <Routes>
            <Route path="/" element={<Navigate to="/console" />} />
            <Route path="/scripts" element={<ScriptsPage/>} />
            <Route path="/console" element={<ConsolePage/>} />
        </Routes>
    );
};

export default AppRoutes;
