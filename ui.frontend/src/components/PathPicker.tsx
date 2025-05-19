import { Breadcrumbs, Button, ButtonGroup, Content, Dialog, DialogContainer, Flex, Heading, Item, ListView, Selection, Text } from '@adobe/react-spectrum';
import Document from '@spectrum-icons/workflow/Document';
import FileCode from '@spectrum-icons/workflow/FileCode';
import Folder from '@spectrum-icons/workflow/Folder';
import Home from '@spectrum-icons/workflow/Home';
import Project from '@spectrum-icons/workflow/Project';
import { ReactNode, useCallback, useEffect, useState } from 'react';
import { apiRequest } from '../utils/api';
import { AssistCodeOutput } from '../utils/api.types';

enum NodeType {
  FOLDER = 'nt:folder',
  ORDERED_FOLDER = 'sling:OrderedFolder',
  SLING_FOLDER = 'sling:Folder',
  CQ_PROJECTS = 'cq/projects',
  REDIRECT = 'sling:redirect',
  ACL = 'rep:ACL',
  PAGE = 'cq:Page',
}

const FOLDER_NODE_TYPES = [NodeType.FOLDER, NodeType.ORDERED_FOLDER, NodeType.SLING_FOLDER, NodeType.CQ_PROJECTS, NodeType.REDIRECT, NodeType.ACL] as const;

interface PathItem {
  id: string;
  name: string;
  path: string;
  type: string;
  hasChildren?: boolean;
}

interface PathPickerProps {
  open?: boolean;
  onSelect: (path: string) => void;
  onCancel: () => void;
  label?: ReactNode;
  basePath?: string;
  confirmButtonLabel?: ReactNode;
}

const getIconForType = (type: string) => {
  switch (type) {
    case 'sling:Folder':
      return <Folder gridRow="1 / span 2" marginEnd="size-100" size="M" />;
    case 'cq:Page':
      return <FileCode gridRow="1 / span 2" marginEnd="size-100" size="M" />;
    case 'nt:file':
      return <Document gridRow="1 / span 2" marginEnd="size-100" size="M" />;
    case 'sling:OrderedFolder':
      return <Project gridRow="1 / span 2" marginEnd="size-100" size="M" />;
    default:
      return <Document gridRow="1 / span 2" marginEnd="size-100" size="M" />;
  }
};

const PathPicker = ({ onSelect, onCancel, label = 'Select Path', basePath = '/', confirmButtonLabel = 'Confirm', open }: PathPickerProps) => {
  const [selectedItemData, setSelectedItemData] = useState<PathItem | null>(null);
  const [path, setPath] = useState<string>(basePath);
  const [isLoading, setIsLoading] = useState(false);
  const [loadedPaths, setLoadedPaths] = useState<Record<string, PathItem[]>>({});

  useEffect(() => {
    const fetchItems = async () => {
      setSelectedItemData(null);
      if (loadedPaths[path]) {
        return;
      }

      setIsLoading(true);

      try {
        const response = await apiRequest<AssistCodeOutput>({
          operation: 'Path search',
          url: `/apps/acm/api/assist-code.json?type=resource&word=${encodeURIComponent(path)}/`,
          method: 'get',
        });

        const responseData = response.data;
        const responseItems = responseData?.data.suggestions;

        if (responseItems && Array.isArray(responseItems)) {
          const newItems = responseItems.map((item) => {
            const resourceTypeMatch = item.i.match(/Resource Type: (.+)/);
            const resourceType = resourceTypeMatch ? resourceTypeMatch[1] : 'unknown';
            const isFolderLike = FOLDER_NODE_TYPES.some((t) => resourceType.includes(t));
            const isPage = resourceType === NodeType.PAGE;
            const name = item.it.split('/').pop() || item.it;

            return {
              id: item.it,
              name: name,
              path: item.it,
              type: resourceType,
              hasChildren: isFolderLike || isPage,
            };
          });

          setLoadedPaths((prev) => ({ ...prev, [path]: newItems }));
          setPath(path);
        }
      } catch (error) {
        console.error('Error fetching paths:', error);
      } finally {
        setIsLoading(false);
      }
    };

    fetchItems();

    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [path]);

  const handleItemClick = useCallback((item: PathItem) => {
    if (item.hasChildren) {
      setPath(item.path);
    }
  }, []);

  const handleSelectionChange = (keys: Selection) => {
    if (keys === 'all') {
      return;
    }

    if (keys.size !== 1) {
      return;
    }

    const selectedKeys = Array.from(keys);
    const key = String(selectedKeys[0]);
    const item = loadedPaths[path].find((item) => item.id === key);

    if (item) {
      setSelectedItemData(item);
    }
  };

  const handleConfirm = useCallback(() => {
    if (selectedItemData) {
      onSelect(selectedItemData.path);
    }
  }, [selectedItemData, onSelect]);

  const handleAction = (key: React.Key) => {
    const item = loadedPaths[path].find((item) => item.id === key);

    if (item?.hasChildren) {
      handleItemClick(item);
    }
  };

  return (
    <DialogContainer onDismiss={onCancel}>
      {open && (
        <Dialog>
          <Heading>
            <Flex direction="column" gap="size-100">
              <Text>{label}</Text>
              <Breadcrumbs marginTop="size-100" showRoot size="M" isDisabled={isLoading} onAction={(p) => setPath(p.toString())}>
                {path.split('/').map((p, index) => {
                  const fullPath = path
                    .split('/')
                    .slice(0, index + 1)
                    .join('/');
                  const label = index === 0 ? <Home size="S" /> : p;

                  return <Item key={fullPath}>{label}</Item>;
                })}
              </Breadcrumbs>
            </Flex>
          </Heading>
          <Content>
            <ListView
              aria-label="Path items"
              density="compact"
              selectionMode="single"
              selectionStyle="highlight"
              selectedKeys={selectedItemData ? [selectedItemData.id] : undefined}
              onSelectionChange={handleSelectionChange}
              items={loadedPaths[path] ?? []}
              onAction={handleAction}
            >
              {(item) => (
                <Item key={item.id} textValue={item.name} hasChildItems={item.hasChildren}>
                  <Text>{item.name}</Text>
                  {getIconForType(item.type)}
                  <Text slot="description">{item.type}</Text>
                </Item>
              )}
            </ListView>
          </Content>
          <ButtonGroup>
            <Button variant="secondary" onPress={onCancel} isDisabled={isLoading}>
              Cancel
            </Button>
            <Button variant="accent" onPress={handleConfirm} isDisabled={!selectedItemData || isLoading}>
              {confirmButtonLabel}
            </Button>
          </ButtonGroup>
        </Dialog>
      )}
    </DialogContainer>
  );
};

export default PathPicker;
