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
    ButtonGroup,
    ProgressBar,
    Tabs,
    TabList,
    TabPanels,
    Item,
    Text,
    Heading,
    Content,
    IllustratedMessage
} from "@adobe/react-spectrum";
import { Key, Selection } from '@react-types/shared';
import { toastRequest } from '../utils/api';
import { DataScript } from '../utils/api.types';
import Cancel from "@spectrum-icons/workflow/Cancel";
import PlayCircle from "@spectrum-icons/workflow/PlayCircle";
import CheckmarkCircle from "@spectrum-icons/workflow/CheckmarkCircle";
import CloseCircle from "@spectrum-icons/workflow/CloseCircle";
import NotFound from '@spectrum-icons/illustrations/NotFound';

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
        const params = new URLSearchParams();
        params.append('action', action);
        params.append('type', showEnabled ? 'enabled' : 'disabled');
        paths.forEach(path => params.append('path', path.toString()));
        
        try {
            await toastRequest({
                method: 'POST',
                url: `/apps/contentor/api/script.json?${params.toString()}`,
                operation: `${action} scripts`,
            });
            loadScripts(showEnabled ? 'enabled' : 'disabled');
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
        <Flex direction="column" gap="size-400">
            <Tabs aria-label='Scripts' onSelectionChange={(selected) => setShowEnabled(selected === 'enabled')}>
                <TabList>
                    <Item aria-label="Enabled scripts" key="enabled"><CheckmarkCircle /><Text>Enabled</Text></Item>
                    <Item aria-label="Disabled scripts" key="disabled"><CloseCircle /><Text>Disabled</Text></Item>
                </TabList>
                <TabPanels>
                    <Item key="enabled">
                        <Flex direction="column" gap="size-200" marginY="size-100">
                            <View>
                                <Flex justifyContent="space-between" alignItems="center">
                                    <ButtonGroup>
                                        <Button
                                            variant="negative"
                                            isDisabled={!selectedKeys || selectedKeys === 'all' || (selectedKeys as Set<Key>).size === 0}
                                            onPress={handleToggleScripts}
                                        >
                                            <Cancel />
                                            <Text>Disable</Text>
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
                    </Item>
                    <Item key="disabled">
                        <Flex direction="column" gap="size-200" marginY="size-100">
                            <View>
                                <Flex justifyContent="space-between" alignItems="center">
                                    <ButtonGroup>
                                        <Button
                                            variant="accent"
                                            isDisabled={!selectedKeys || selectedKeys === 'all' || (selectedKeys as Set<Key>).size === 0}
                                            onPress={handleToggleScripts}
                                        >
                                            <PlayCircle />
                                            <Text>Enable</Text>
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
                    </Item>
                </TabPanels>
            </Tabs>
        </Flex>
    );
};

export default ScriptsPage;
