import { Content, Flex, Heading, IllustratedMessage, Item, ProgressBar, TabList, TabPanels, Tabs, Text, View } from '@adobe/react-spectrum';
import NotFound from '@spectrum-icons/illustrations/NotFound';
import FolderOpen from '@spectrum-icons/workflow/FolderOpen';
import FolderOpenOutline from '@spectrum-icons/workflow/FolderOpenOutline';
import { useEffect, useState } from 'react';
import Markdown from '../components/Markdown';
import SnippetCode from '../components/SnippetCode';
import { useNavigationTab } from '../hooks/navigation';
import { toastRequest } from '../utils/api';
import { Snippet, SnippetOutput } from '../utils/api.types';

const SnippetsPage = () => {
  const [snippets, setSnippets] = useState<SnippetOutput | null>(null);

  useEffect(() => {
    toastRequest<SnippetOutput>({
      method: 'GET',
      url: `/apps/acm/api/snippet.json`,
      operation: 'Snippets loading',
      positive: false,
    })
      .then((data) => setSnippets(data.data.data))
      .catch((error) => console.error('Snippets loading error:', error));
  }, []);

  const defaultTab = snippets?.list[0]?.group;
  const [selectedTab, handleTabChange] = useNavigationTab(defaultTab);

  if (snippets === null) {
    return (
      <Flex flex="1" justifyContent="center" alignItems="center">
        <ProgressBar label="Loading..." isIndeterminate />
      </Flex>
    );
  }

  if (snippets.list.length === 0) {
    return (
      <Flex direction="column" flex="1">
        <IllustratedMessage>
          <NotFound />
          <Content>No snippets found</Content>
        </IllustratedMessage>
      </Flex>
    );
  }

  const snippetGroups = snippets.list.reduce(
    (groups: { [key: string]: Snippet[] }, snippet) => {
      if (!groups[snippet.group]) {
        groups[snippet.group] = [];
      }
      groups[snippet.group].push(snippet);
      return groups;
    },
    {} as { [key: string]: Snippet[] },
  );
  const snippetGroupIcons = [<FolderOpen />, <FolderOpenOutline />];

  return (
    <Tabs aria-label="Snippet Groups" selectedKey={selectedTab} onSelectionChange={handleTabChange}>
      <TabList>
        {Object.keys(snippetGroups)
          .sort()
          .map((group, groupIndex) => (
            <Item key={group}>
              {snippetGroupIcons[groupIndex % snippetGroupIcons.length]}
              <Text>{group}</Text>
            </Item>
          ))}
      </TabList>
      <TabPanels>
        {Object.keys(snippetGroups)
          .sort()
          .map((group) => (
            <Item key={group}>
              <Flex direction="column" flex="1" gap="size-100" marginY="size-100">
                {snippetGroups[group]
                  .sort((a, b) => a.name.localeCompare(b.name))
                  .map((snippet) => (
                    <View key={snippet.id} backgroundColor="gray-50" borderWidth="thin" borderColor="dark" borderRadius="medium" paddingY="size-100" paddingX="size-200" marginY="size-10">
                      <Heading level={3}>{snippet.name}</Heading>
                      <Content>
                        <Markdown>{snippet.documentation}</Markdown>
                      </Content>
                      <View backgroundColor="gray-800" borderWidth="thin" position="relative" borderColor="dark" borderRadius="medium" marginY="size-100">
                        <SnippetCode content={snippet.content} />
                      </View>
                    </View>
                  ))}
              </Flex>
            </Item>
          ))}
      </TabPanels>
    </Tabs>
  );
};

export default SnippetsPage;
