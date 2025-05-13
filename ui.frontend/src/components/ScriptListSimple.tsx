import React, { useCallback, useEffect, useState } from 'react';
import { Flex, ProgressBar, TableBody, TableHeader, TableView, IllustratedMessage, Content, Row, Cell, Column } from '@adobe/react-spectrum';
import NotFound from '@spectrum-icons/illustrations/NotFound';
import { useNavigate } from 'react-router-dom';
import { toastRequest } from '../utils/api';
import { ScriptOutput, ScriptType } from '../utils/api.types';

type ScriptListSimpleProps = {
    type: ScriptType;
};

const ScriptListSimple: React.FC<ScriptListSimpleProps> = ({ type }) => {
    const [scripts, setScripts] = useState<ScriptOutput | null>(null);
    const [loading, setLoading] = useState<boolean>(true);
    const navigate = useNavigate();

    const loadScripts = useCallback(() => {
        setLoading(true);
        toastRequest<ScriptOutput>({
            method: 'GET',
            url: `/apps/acm/api/script.json?type=${type}`,
            operation: `Scripts loading (${type.toString().toLowerCase()})`,
            positive: false,
        })
            .then((data) => setScripts(data.data.data))
            .catch((error) => console.error(`Scripts loading (${type}) error:`, error))
            .finally(() => setLoading(false));
    }, [type]);

    useEffect(() => {
        loadScripts();
    }, [type, loadScripts]);

    const renderEmptyState = () => (
        <IllustratedMessage>
            <NotFound />
            <Content>No scripts found</Content>
        </IllustratedMessage>
    );

    if (scripts === null || loading) {
        return (
            <Flex flex="1" justifyContent="center" alignItems="center">
                <ProgressBar label="Loading..." isIndeterminate />
            </Flex>
        );
    }

    return (
        <TableView flex="1" aria-label={`Script list (${type})`} renderEmptyState={renderEmptyState} onAction={(key) => navigate(`/scripts/view/${encodeURIComponent(key)}`)}>
            <TableHeader>
                <Column>Name</Column>
            </TableHeader>
            <TableBody>
                {(scripts.list || []).map((script) => (
                    <Row key={script.id}>
                        <Cell>{script.name}</Cell>
                    </Row>
                ))}
            </TableBody>
        </TableView>
    );
};

export default ScriptListSimple;