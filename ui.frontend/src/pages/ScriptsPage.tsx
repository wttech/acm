import React, { useEffect, useState } from 'react';
import {Cell, Column, Flex, Row, TableBody, TableHeader, TableView, View, Heading, ProgressBar} from "@adobe/react-spectrum";
import { toastRequest } from '../utils/api';
import { DataScript } from '../utils/api.types';

const ScriptsPage = () => {
    const [enabledScripts, setEnabledScripts] = useState<DataScript | null>(null);
    const [disabledScripts, setDisabledScripts] = useState<DataScript | null>(null);

    useEffect(() => {
        // Fetch enabled scripts
        toastRequest<DataScript>({
            method: 'GET',
            url: '/apps/contentor/api/script.json?type=enabled',
            operation: 'Fetch enabled scripts',
            positive: false
        })
            .then(data => setEnabledScripts(data.data.data))
            .catch(error => console.error('Error fetching enabled scripts:', error));

        // Fetch disabled scripts
        toastRequest<DataScript>({
            method: 'GET',
            url: '/apps/contentor/api/script.json?type=disabled',
            operation: 'Fetch disabled scripts',
            positive: false
        })
            .then(data => setDisabledScripts(data.data.data))
            .catch(error => console.error('Error fetching disabled scripts:', error));
    }, []);

    if (enabledScripts === null || disabledScripts === null) {
        return (
            <Flex justifyContent="center" alignItems="center" height="100vh">
                <ProgressBar label="Loading..." isIndeterminate />
            </Flex>
        );
    }

    return (
        <Flex direction="column" gap="size-400">
            <View>
                <Heading level={2}>Enabled Scripts</Heading>
                <TableView aria-label="Enabled script list" selectionMode="multiple">
                    <TableHeader>
                        <Column>Name</Column>
                        <Column>Content</Column>
                    </TableHeader>
                    <TableBody>
                        {enabledScripts.list.map((script) => (
                            <Row key={script.id}>
                                <Cell>{script.id}</Cell>
                                <Cell>{script.content}</Cell>
                            </Row>
                        ))}
                    </TableBody>
                </TableView>
            </View>

            <View>
                <Heading level={2}>Disabled Scripts</Heading>
                <TableView aria-label="Disabled script list" selectionMode="multiple">
                    <TableHeader>
                        <Column>Name</Column>
                        <Column>Content</Column>
                    </TableHeader>
                    <TableBody>
                        {disabledScripts.list.map((script) => (
                            <Row key={script.id}>
                                <Cell>{script.id}</Cell>
                                <Cell>{script.content}</Cell>
                            </Row>
                        ))}
                    </TableBody>
                </TableView>
            </View>

};

export default ScriptsPage;
