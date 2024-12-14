import React, {useCallback, useEffect, useState} from 'react';
import {
    Button,
    ButtonGroup,
    Cell,
    Column,
    Content,
    Flex,
    IllustratedMessage,
    ProgressBar,
    Row,
    TableBody,
    TableHeader,
    TableView,
    Text,
    View
} from "@adobe/react-spectrum";
import { Key, Selection } from '@react-types/shared';
import { toastRequest } from '../utils/api';
import { DataScript } from '../utils/api.types';
import Cancel from "@spectrum-icons/workflow/Cancel";
import PlayCircle from "@spectrum-icons/workflow/PlayCircle";
import NotFound from '@spectrum-icons/illustrations/NotFound';

type ScriptListProps = {
    type: 'enabled' | 'disabled';
};

const ScriptList: React.FC<ScriptListProps> = ({ type }) => {
    const [scripts, setScripts] = useState<DataScript | null>(null);
    const [selectedKeys, setSelectedKeys] = useState<Selection>(new Set<Key>());

    const loadScripts = useCallback(() => {
        toastRequest<DataScript>({
            method: 'GET',
            url: `/apps/contentor/api/script.json?type=${type}`,
            operation: `Scripts loading (${type})`,
            positive: false
        })
            .then(data => setScripts(data.data.data))
            .catch(error => console.error(`Scripts loading (${type}) error:`, error));
    }, [type]);

    useEffect(() => {
        loadScripts();
    }, [type, loadScripts]);

    const selectedPaths = (selectedKeys: Selection): string[] => {
        if (selectedKeys === 'all') {
            return scripts?.list.map(script => script.id) || [];
        } else {
            return Array.from(selectedKeys as Set<Key>).map(key => key.toString());
        }
    };

    const toggleScripts = async () => {
        const action = type === 'enabled' ? 'disable' : 'enable';
        const paths = selectedPaths(selectedKeys);

        const params = new URLSearchParams();
        params.append('action', action);
        params.append('type', type);
        paths.forEach(path => params.append('path', path));

        try {
            await toastRequest({
                method: 'POST',
                url: `/apps/contentor/api/script.json?${params.toString()}`,
                operation: `${action} scripts`,
            });
            loadScripts();
            setSelectedKeys(new Set<Key>());
        } catch (error) {
            console.error(`${action} scripts error:`, error);
        }
    };

    const renderEmptyState = () => (
        <IllustratedMessage>
            <NotFound />
            <Content>No scripts found</Content>
        </IllustratedMessage>
    );

    if (scripts === null) {
        return (
            <Flex justifyContent="center" alignItems="center" height="100vh">
                <ProgressBar label="Loading..." isIndeterminate />
            </Flex>
        );
    }

    return (
        <Flex direction="column" gap="size-200" marginY="size-100">
            <View>
                <Flex justifyContent="space-between" alignItems="center">
                    <ButtonGroup>
                        <Button
                            variant={type === 'enabled' ? 'negative' : 'accent'}
                            isDisabled={selectedKeys === 'all' ? false : (selectedKeys as Set<Key>).size === 0}
                            onPress={toggleScripts}
                        >
                            {type === 'enabled' ? <Cancel /> : <PlayCircle />}
                            <Text>{type === 'enabled' ? 'Disable' : 'Enable'}</Text>
                        </Button>
                    </ButtonGroup>
                </Flex>
            </View>
            <TableView
                aria-label="Scripts list"
                selectionMode="multiple"
                selectedKeys={selectedKeys}
                onSelectionChange={setSelectedKeys}
                minHeight="size-3600"
                renderEmptyState={renderEmptyState}
            >
                <TableHeader>
                    <Column>Name</Column>
                    <Column>Content</Column>
                </TableHeader>
                <TableBody>
                    {(scripts.list || []).map(script => (
                        <Row key={script.id}>
                            <Cell>{script.name}</Cell>
                            <Cell>{script.content}</Cell>
                        </Row>
                    ))}
                </TableBody>
            </TableView>
        </Flex>
    );
};

export default ScriptList;
