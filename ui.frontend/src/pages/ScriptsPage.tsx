import React, { Key, useEffect, useState } from 'react';
import {
    Cell,
    Column,
    Flex,
    Row,
    TableBody,
    TableHeader,
    TableView,
    View,
    ProgressBar,
    ActionButton,
    Switch,
    ButtonGroup,
    Button
} from "@adobe/react-spectrum";
import { toastRequest } from '../utils/api';
import { DataScript } from '../utils/api.types';

type ScriptType = 'enabled' | 'disabled';

const ScriptsPage = () => {
    const [scripts, setScripts] = useState<DataScript | null>(null);
    const [scriptType, setScriptType] = useState<ScriptType>('enabled');
    const [selectedKeys, setSelectedKeys] = useState<Set<Key>>(new Set());
    const [isLoading, setIsLoading] = useState(false);

    const fetchScripts = async (type: ScriptType) => {
        setIsLoading(true);
        try {
            const response = await toastRequest<DataScript>({
                method: 'GET',
                url: `/apps/contentor/api/script.json?type=${type}`,
                operation: `Fetch ${type} scripts`,
                positive: false
            });
            setScripts(response.data.data);
        } catch (error) {
            console.error(`Error fetching ${type} scripts:`, error);
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        fetchScripts(scriptType);
    }, [scriptType]);

    const handleScriptTypeChange = (isChecked: boolean) => {
        setScriptType(isChecked ? 'enabled' : 'disabled');
        setSelectedKeys(new Set());
    };

    const handleBulkAction = async () => {
        if (selectedKeys.size === 0) return;

        setIsLoading(true);
        const targetType = scriptType === 'enabled' ? 'disabled' : 'enabled';
        const selectedIds = Array.from(selectedKeys);

        try {
            await toastRequest({
                method: 'POST',
                url: `/apps/contentor/api/script/${targetType}.json`,
                operation: `${targetType === 'enabled' ? 'Enable' : 'Disable'} selected scripts`,
                data: { ids: selectedIds }
            });
            await fetchScripts(scriptType);
            setSelectedKeys(new Set());
        } catch (error) {
            console.error('Error updating scripts:', error);
        } finally {
            setIsLoading(false);
        }
    };

    if (isLoading && !scripts) {
        return (
            <Flex justifyContent="center" alignItems="center" height="100vh">
                <ProgressBar label="Loading..." isIndeterminate />
            </Flex>
        );
    }

    const actionButtonLabel = scriptType === 'enabled' ? 'Disable Selected' : 'Enable Selected';

    return (
        <Flex direction="column" gap="size-200">
            <Flex direction="row" gap="size-200" alignItems="center" marginBottom="size-200">
                <Switch
                    isSelected={scriptType === 'enabled'}
                    onChange={handleScriptTypeChange}
                >
                    {scriptType === 'enabled' ? 'Enabled Scripts' : 'Disabled Scripts'}
                </Switch>
                <ButtonGroup>
                    <Button
                        variant="primary"
                        isDisabled={selectedKeys.size === 0 || isLoading}
                        onPress={handleBulkAction}
                    >
                        {actionButtonLabel}
                    </Button>
                </ButtonGroup>
            </Flex>

            <TableView
                aria-label="Scripts list"
                selectionMode="multiple"
                selectedKeys={selectedKeys}
                onSelectionChange={setSelectedKeys}
                isQuiet
            >
                <TableHeader>
                    <Column>Name</Column>
                    <Column>Content</Column>
                </TableHeader>
                <TableBody>
                    {scripts?.list.map((script) => (
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
