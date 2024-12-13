import React, { useEffect, useState } from 'react';
import {
    Cell,
    Column,
    Flex,
    Row,
    TableBody,
    TableHeader,
    TableView,
    View,
    Button,
    ProgressBar,
    Switch
} from "@adobe/react-spectrum";
import { Key, Selection } from '@react-types/shared';
import { toastRequest } from '../utils/api';
import { DataScript } from '../utils/api.types';

const ScriptsPage = () => {
    const [scripts, setScripts] = useState<DataScript | null>(null);
    const [showEnabled, setShowEnabled] = useState<boolean>(true);
    const [selectedKeys, setSelectedKeys] = useState<Selection>(new Set<Key>());

    const loadScripts = (type: string) => {
        toastRequest<DataScript>({
            method: 'GET',
            url: `/apps/contentor/api/script.json?type=${type}`,
            operation: `Scripts loading (${type})`,
            positive: false
        })
            .then(data => setScripts(data.data.data))
            .catch(error => console.error(`Scripts loading (${type}) error:`, error));
    };

    useEffect(() => {
        loadScripts(showEnabled ? 'enabled' : 'disabled');
    }, [showEnabled]);

    const handleToggleScripts = async () => {
        const action = showEnabled ? 'disable' : 'enable';
        const paths = Array.from(selectedKeys as Set<Key>);
        const pathsQuery = paths.map(script => `path=${script}`).join('&');
        
        try {
            await toastRequest({
                method: 'POST',
                url: `/apps/contentor/api/script.json?type=${showEnabled ? 'enabled' : 'disabled'}&action=${action}&${pathsQuery}`,
                operation: `${action} selected scripts`,
            });
            loadScripts(showEnabled ? 'enabled' : 'disabled');
            setSelectedKeys(new Set<Key>());
        } catch (error) {
            console.error(`Error ${action}ing scripts:`, error);
        }
    };

    if (scripts === null) {
        return (
            <Flex justifyContent="center" alignItems="center" height="100vh">
                <ProgressBar label="Loading..." isIndeterminate />
            </Flex>
        );
    }

    return (
        <Flex direction="column" gap="size-400">
            <Flex justifyContent="space-between" alignItems="center" marginBottom="size-200">
                <Button
                    variant="primary"
                    isDisabled={!selectedKeys || selectedKeys === 'all' || (selectedKeys as Set<Key>).size === 0}
                    onPress={handleToggleScripts}
                >
                    {showEnabled ? 'Disable' : 'Enable'} Selected
                </Button>
                <Switch
                    isSelected={showEnabled}
                    onChange={setShowEnabled}
                >
                    Show {showEnabled ? 'Enabled' : 'Disabled'} Scripts
                </Switch>
            </Flex>
            
            <TableView
                aria-label="Scripts list"
                selectionMode="multiple"
                selectedKeys={selectedKeys}
                onSelectionChange={setSelectedKeys}
            >
                <TableHeader>
                    <Column>Name</Column>
                    <Column>Content</Column>
                </TableHeader>
                <TableBody>
                    {scripts.list.map((script) => (
                        <Row key={script.id}>
                            <Cell>{script.id}</Cell>
                            <Cell>{script.content}</Cell>
                        </Row>
                    ))}
                </TableBody>
            </TableView>
        </Flex>
    );
};

export default ScriptsPage;
