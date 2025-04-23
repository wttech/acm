import { SpectrumTreeViewProps, Text, TreeView, TreeViewItem, TreeViewItemContent } from '@adobe/react-spectrum';
import { Selection } from '@react-types/shared/src/selection';
import { useEffect, useState } from 'react';
import { apiRequest } from '../utils/api.ts';
import { AssistCodeOutput, AssistCodeOutputSuggestion } from '../utils/api.types.ts';
import styles from './PathInput.module.css';

interface Node {
  name: string;
  children: Node[] | null;
}

interface IPathInput extends SpectrumTreeViewProps<string> {
  rootPath: string;
  onChange: (paths: string) => void;
  errorMessage: string | undefined;
  isInvalid: boolean;
  label: string;
  value: string;
}

export function PathInput({ rootPath = '', onChange, label, value, ...props }: IPathInput) {
  const { getPathCompletion } = useRequestPathCompletion();
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [expanded, setExpanded] = useState<Set<string>>(new Set());
  const [items, setItems] = useState<Node>({
    name: rootPath,
    children: null,
  });

  const onSelectionChange = (keys: Selection) => {
    setSelected((prevState) => {
      const newSelection = new Set([...keys].map((key) => key.toString()));
      if ((prevState.size === newSelection.size && [...newSelection].every((value) => prevState.has(value))) || keys == 'all') {
        onChange([...prevState][0]);
        return prevState;
      }
      onChange([...newSelection][0]);
      return newSelection;
    });
  };

  const translateSuggestion = (suggestion: AssistCodeOutputSuggestion): Node => {
    const name = suggestion.it.split('/').at(-1);
    return { name: name ? name : suggestion.it, children: null };
  };

  const findAndUpdateNodeFromPath = (path: string, newChildren: Node[]) => {
    const updateTree = (node: Node, pathParts: string[]): Node => {
      if (pathParts.length === 0) {
        return { ...node, children: newChildren };
      }
      const [currentPart, ...remainingParts] = pathParts;
      const children = node.children || [];
      let child = children.find((child) => child.name === currentPart);
      if (!child) {
        child = { name: currentPart, children: [] };
        children.push(child);
      }

      return {
        ...node,
        children: children.map((c) => (c.name === currentPart ? updateTree(c, remainingParts) : c)),
      };
    };
    const rootName = items.name;
    const normalizedPath = path.startsWith(rootName) ? path.slice(rootName.length) : path;
    const parts = normalizedPath.split('/').filter(Boolean);
    setItems((prevItems) => updateTree(prevItems, parts));
  };

  const loadSuggestions = async (path: string) => {
    getPathCompletion(path)
      .then((data) => data.suggestions.map(translateSuggestion))
      .then((nodes) => {
        findAndUpdateNodeFromPath(path, nodes);
      });
  };

  const loadedPaths = new Set<string>([rootPath]);
  const onExpandedChange = (keys: Selection) => {
    const newKeys = new Set([...keys].map((key) => key.toString()));
    const differenceToAdd = [...newKeys].filter((key) => !expanded.has(key));
    const differenceToRemove = [...expanded].filter((key) => !newKeys.has(key));

    const updatedExpanded = new Set(expanded);

    differenceToAdd.forEach(async (key) => {
      if (!loadedPaths.has(key)) {
        loadedPaths.add(key);
        await loadSuggestions(key + '/');
      }
      updatedExpanded.add(key);
    });

    differenceToRemove.forEach((key) => {
      updatedExpanded.delete(key);
    });

    setExpanded(updatedExpanded);
  };

  useEffect(() => {
    const initializePathInput = async () => {
      if (!loadedPaths.has(rootPath)) {
        loadedPaths.add(rootPath);
        await loadSuggestions(rootPath);
      }
      const expandPathToValue = async (path: string) => {
        const pathParts = path.split('/').filter(Boolean);
        let currentPath = rootPath;
        const expandedKeys = new Set<string>();
        expandedKeys.add(rootPath);
        for (const part of pathParts) {
          currentPath += `/${part}`;
          expandedKeys.add(currentPath);
          if (!loadedPaths.has(currentPath)) {
            loadedPaths.add(currentPath);
            await loadSuggestions(currentPath);
          }
        }
        return expandedKeys;
      };
      const expandedKeys = await expandPathToValue(value);
      setExpanded(expandedKeys);
      setSelected(new Set([value]));
    };

    initializePathInput().catch((err) => {
      console.error('Error initializing PathInput:', err);
    });
  }, [rootPath, value]);

  return (
    <>
      <p className={styles.label}>{label}</p>
      <TreeView {...props} maxHeight={'size-2400'} width={'100%'} onSelectionChange={onSelectionChange} onExpandedChange={onExpandedChange} defaultSelectedKeys={new Set(value)} selectedKeys={selected} expandedKeys={expanded}>
        {items && <TreeItem node={items} path={''} />}
      </TreeView>
    </>
  );
}

const TreeItem = ({ node, path }: { node: Node; path: string }) => {
  return (
    <TreeViewItem id={path + node.name} textValue={node.name}>
      <TreeViewItemContent>
        <Text>{node.name}</Text>
      </TreeViewItemContent>
      {node.children && node.children.length > 0 ? (
        node.children.map((child: Node) => <TreeItem node={child} path={path + node.name + '/'} key={path + node.name + child.name} />)
      ) : node.children ? null : (
        <TreeViewItem children={[]} textValue={'empty'} />
      )}
    </TreeViewItem>
  );
};

const useRequestPathCompletion = () => {
  async function getPathCompletion(path: string) {
    if (!path.endsWith('/')) {
      path += '/';
    }
    const response = await apiRequest<AssistCodeOutput>({
      method: 'GET',
      url: `/apps/acm/api/assist-code.json?type=resource&word=${encodeURIComponent(path)}`,
      operation: 'Code assistance',
    });
    return response.data.data;
  }

  return { getPathCompletion };
};
