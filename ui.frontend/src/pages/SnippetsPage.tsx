import React, { useEffect, useState } from 'react';
import {Flex, View, Heading, Content, ProgressBar, IllustratedMessage} from '@adobe/react-spectrum';
import { toastRequest } from '../utils/api';
import { SnippetOutput } from '../utils/api.types';
import NotFound from "@spectrum-icons/illustrations/NotFound";

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
            <Flex flex="1" justifyContent="center" alignItems="center" height="100vh">
                <ProgressBar label="Loading..." isIndeterminate />
            </Flex>
        )
    }

    if (snippets.list.length === 0) {
        return (
            <Flex direction="column" flex="1">
                <IllustratedMessage>
                    <NotFound />
                    <Content>No snippets found</Content>
                </IllustratedMessage>
            </Flex>
        )
    }

    return (
        <Flex direction="column" flex="1" gap="size-200">
            {snippets.list.map(snippet => (
                <View
                    key={snippet.id}
                    backgroundColor="gray-50"
                    borderWidth="thin"
                    borderColor="dark"
                    borderRadius="medium"
                    padding="size-200"
                    marginY="size-10"
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
