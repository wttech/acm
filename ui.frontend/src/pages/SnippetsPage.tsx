import React, { useEffect, useState } from 'react';
import { Flex, View, Heading, Content, Button } from '@adobe/react-spectrum';
import { toastRequest } from '../utils/api'; // Adjust the import path as necessary
import { DataSnippet } from '../utils/api.types'; // Adjust the import path as necessary

const SnippetsPage = () => {
    const [snippets, setSnippets] = useState<DataSnippet | null>(null);

    useEffect(() => {
        toastRequest<DataSnippet>({
            method: 'GET',
            url: `/apps/contentor/api/snippet.json`,
            operation: 'Fetch snippets',
            positive: false
        })
            .then(data => setSnippets(data.data.data))
            .catch(error => console.error('Error fetching snippets:', error));
    }, []);

    if (snippets === null) {
        return <div>Loading...</div>; // TODO do it more nicely
    }

    return (
        <Flex direction="column" gap="size-200">
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
                        <pre>{snippet.content}</pre>
                    </Content>
                    <Flex justifyContent="end">
                        <Button variant="cta">Action</Button>
                    </Flex>
                </View>
            ))}
        </Flex>
    );
};

export default SnippetsPage;
