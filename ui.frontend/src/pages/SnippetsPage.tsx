import React, { useEffect, useState } from 'react';
import {Flex, View, Heading, Content, Text, ProgressBar} from '@adobe/react-spectrum';
import { toastRequest } from '../utils/api'; // Adjust the import path as necessary
import { SnippetOutput } from '../utils/api.types'; // Adjust the import path as necessary

const SnippetsPage = () => {
    const [snippets, setSnippets] = useState<SnippetOutput | null>(null);

    useEffect(() => {
        toastRequest<SnippetOutput>({
            method: 'GET',
            url: `/apps/contentor/api/snippet.json`,
            operation: 'Snippets loading',
            positive: false
        })
            .then(data => setSnippets(data.data.data))
            .catch(error => console.error('Snippets loading error:', error));
    }, []);

    if (snippets === null) {
        return (
            <Flex justifyContent="center" alignItems="center" height="100vh">
                <ProgressBar label="Loading..." isIndeterminate />
            </Flex>
        )
    }

    return (
        <Flex direction="column" gap="size-200">
            {!snippets.list.length && <Heading level={2}>No snippets found</Heading>}
            {snippets.list.map(snippet => (
                <View
                    key={snippet.id}
                    backgroundColor="gray-100"
                    borderWidth="thin"
                    borderColor="dark"
                    borderRadius="medium"
                    padding="size-200"
                    marginBottom="size-200"
                >
                    <Heading level={3}>{snippet.name}</Heading>
                    <Content>
                        {snippet.documentation}
                    </Content>
                    <Content>
                        <pre style={{fontSize: '80%'}}>{snippet.content}</pre>
                    </Content>
                </View>
            ))}
        </Flex>
    );
};

export default SnippetsPage;
