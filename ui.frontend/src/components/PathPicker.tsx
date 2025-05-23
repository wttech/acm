import { Breadcrumbs, Button, ButtonGroup, Content, ContextualHelp, Dialog, DialogContainer, Flex, Heading, IllustratedMessage, Item, ListView, Selection, SpectrumTextFieldProps, Text, TextField } from '@adobe/react-spectrum';
import { Key } from '@react-types/shared';
import NoSearchResults from '@spectrum-icons/illustrations/NoSearchResults';
import Document from '@spectrum-icons/workflow/Document';
import FileCode from '@spectrum-icons/workflow/FileCode';
import Folder from '@spectrum-icons/workflow/Folder';
import FolderSearch from '@spectrum-icons/workflow/FolderSearch';
import Home from '@spectrum-icons/workflow/Home';
import Project from '@spectrum-icons/workflow/Project';
import { ReactNode, useCallback, useEffect, useRef, useState } from 'react';
import { apiRequest } from '../utils/api';
import { AssistCodeOutput, JCR_CONSTANTS, NodeType } from '../utils/api.types';
import LoadingWrapper from './LoadingWrapper.tsx';

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
  root?: string;
  confirmButtonLabel?: ReactNode;
}

type PathPickerFieldProps = {
  onSelect: (value: string) => void;
  value?: string;
  root?: string;
  label?: ReactNode;
} & SpectrumTextFieldProps;

const getIconForType = (type: string) => {
  switch (type) {
    case NodeType.FOLDER:
    case NodeType.SLING_FOLDER:
    case NodeType.ORDERED_FOLDER:
      return <Folder gridRow="1 / span 2" marginEnd="size-100" size="M" />;
    case NodeType.PAGE:
      return <FileCode gridRow="1 / span 2" marginEnd="size-100" size="M" />;
    case NodeType.FILE:
      return <Document gridRow="1 / span 2" marginEnd="size-100" size="M" />;
    case NodeType.CQ_PROJECTS:
      return <Project gridRow="1 / span 2" marginEnd="size-100" size="M" />;
    default:
      return <Document gridRow="1 / span 2" marginEnd="size-100" size="M" />;
  }
};

export const PathPicker = ({ onSelect, onCancel, label = 'Select Path', root = '', confirmButtonLabel = 'Confirm', open }: PathPickerProps) => {
  const [selectedItemData, setSelectedItemData] = useState<PathItem | null>(null);
  const [loadingPath, setLoadingPath] = useState<string>(root);
  const [loadedPath, setLoadedPath] = useState<string>(root);
  const [isLoading, setIsLoading] = useState(false);
  const loadedPaths = useRef<Record<string, PathItem[]>>({});

  useEffect(() => {
    const fetchItems = async () => {
      setSelectedItemData(null);
      if (loadedPaths.current[loadingPath]) {
        setLoadedPath(loadingPath);
        return;
      }

      setIsLoading(true);

      try {
        const response = await apiRequest<AssistCodeOutput>({
          operation: 'Path search',
          url: `/apps/acm/api/assist-code.json?type=resource&word=${encodeURIComponent(loadingPath + '/')}`,
          method: 'get',
        });

        const responseData = response.data;
        const responseItems = responseData?.data.suggestions;

        if (responseItems && Array.isArray(responseItems)) {
          loadedPaths.current[loadingPath] = responseItems
            .map((item) => {
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
            })
            .sort((a, b) => {
              if (a.name === JCR_CONSTANTS.JCR_CONTENT) {
                return -1;
              }

              if (b.name === JCR_CONSTANTS.JCR_CONTENT) {
                return 1;
              }

              const isFolderA = FOLDER_NODE_TYPES.some((t) => a.type.includes(t));
              const isFolderB = FOLDER_NODE_TYPES.some((t) => b.type.includes(t));

              if (isFolderA && !isFolderB) return -1;
              if (!isFolderA && isFolderB) return 1;

              return a.name.localeCompare(b.name);
            });
          setLoadedPath(loadingPath);
        }
      } catch (error) {
        console.error('Error fetching paths:', error);
      } finally {
        setIsLoading(false);
      }
    };

    fetchItems();
  }, [loadingPath]);

  const handleItemClick = useCallback((item: PathItem) => {
    setLoadingPath(item.path);
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
    const item = loadedPaths.current[loadedPath]?.find((item) => item.id === key);

    if (item) {
      setSelectedItemData(item);
    }
  };

  const handleConfirm = useCallback(() => {
    if (selectedItemData) {
      onSelect(selectedItemData.path);
    }
  }, [selectedItemData, onSelect]);

  const handleAction = (key: Key) => {
    const item = loadedPaths.current[loadedPath]?.find((item) => item.id === key);

    if (item?.hasChildren) {
      handleItemClick(item);
    }
  };

  return (
    <DialogContainer onDismiss={onCancel}>
      {open && (
        <Dialog height="60vh">
          <Heading>
            <Flex direction="column" gap="size-100">
              <Flex gap="size-50" alignItems="center">
                <Text>{label}</Text>
                <ContextualHelp variant="help">
                  <Heading>Repository browsing</Heading>
                  <Content>
                    <Text>
                      <p>Double-click on a folder to open it. Single-click on a resource to select it. After selecting, copy resource path to the clipboard and paste it in the desired text fields.</p>
                      <p>Use the breadcrumbs to navigate back.</p>
                    </Text>
                  </Content>
                </ContextualHelp>
              </Flex>
              <Breadcrumbs marginTop="size-100" showRoot size="M" isDisabled={isLoading} onAction={(p) => setLoadingPath(p.toString())}>
                {loadingPath
                  .replace(root, '')
                  .split('/')
                  .map((p, index) => {
                    const fullPath = loadingPath
                      .split('/')
                      .slice(0, index + 1)
                      .join('/');
                    const label = index === 0 ? <Home size="S" /> : p;

                    return <Item key={index === 0 ? root : fullPath}>{label}</Item>;
                  })}
              </Breadcrumbs>
            </Flex>
          </Heading>
          <Content>
            <LoadingWrapper isRefreshing={isLoading}>
              {loadedPaths.current[loadedPath]?.length || isLoading ? (
                <ListView
                  aria-label="Path items"
                  density="compact"
                  selectionMode="single"
                  selectionStyle="highlight"
                  selectedKeys={selectedItemData ? [selectedItemData.id] : []}
                  onSelectionChange={handleSelectionChange}
                  items={loadedPaths.current[loadedPath]}
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
              ) : (
                <IllustratedMessage>
                  <NoSearchResults />
                  <Content>The selected folder is empty. Please navigate to a different location or go back.</Content>
                </IllustratedMessage>
              )}
            </LoadingWrapper>
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

const PathPickerField = ({ onSelect, root, ...props }: PathPickerFieldProps) => {
  const [pathPickerOpened, setPathPickerOpened] = useState(false);

  const handleSelectPath = (path: string) => {
    onSelect(path);
    setPathPickerOpened(false);
  };

  return (
    <Flex gap="size-100">
      <TextField flexGrow={1} isReadOnly {...props} />
      <Button variant="secondary" style="outline" onPress={() => setPathPickerOpened(true)} aria-label="Pick a path" marginTop="size-300">
        <FolderSearch />
      </Button>
      <PathPicker onSelect={handleSelectPath} onCancel={() => setPathPickerOpened(false)} confirmButtonLabel="Choose" root={root} open={pathPickerOpened} />
    </Flex>
  );
};

export default PathPickerField;
