import { SpectrumTreeViewProps, Text, TreeView, TreeViewItem, TreeViewItemContent } from '@adobe/react-spectrum';
import { Selection } from '@react-types/shared/src/selection';
import React, {forwardRef, Ref, useCallback, useEffect, useState} from 'react';
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

export const PathInput =  forwardRef(function PathInput({ rootPath = "", onChange, label, value, ...props }: IPathInput, ref: Ref<HTMLDivElement>) {
  const { getPathCompletion } = useRequestPathCompletion();
  const [loadedPaths, setLoadedPaths] = useState<Set<string>>(new Set());
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [expanded, setExpanded] = useState<Set<string>>(new Set());
  const [items, setItems] = useState<Node>({
    name: rootPath == "/" ? "" : rootPath,
    children: null,
  });

  const onSelectionChange = (keys: Selection) => {
    const newSelection = new Set([...keys].map((key) => key.toString()));
    const selectedValue = [...newSelection][0];
    if (selectedValue != undefined) {
      setSelected(new Set([selectedValue]));
      // Offload the onChange call to avoid blocking the UI
      setTimeout(() => {
        onChange(selectedValue.length > 0 ? selectedValue : '/');
      }, 0)
    }
  };

  const translateSuggestion = (suggestion: AssistCodeOutputSuggestion): Node => {
    const name = suggestion.it.split('/').at(-1);
    return { name: name ? name : suggestion.it, children: null };
  };

  const findAndUpdateNodeFromPath = (path: string, newChildren: Node[]) => {
    const updateTree = (node: Node, pathParts: string[]): Node => {
      if (pathParts.length === 0) {
        return { ...node, children: newChildren.length > 0 ? newChildren : [] };
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

  const loadSuggestions = useCallback(async (path: string) => {
    await getPathCompletion(path)
      .then((data) => data.suggestions.map(translateSuggestion))
      .then((nodes) => {
        findAndUpdateNodeFromPath(path, nodes);
      });
  }, [])

  const onExpandedChange = (keys: Selection) => {
    const newKeys = new Set([...keys].map((key) => key.toString()));
    const differenceToAdd = [...newKeys].filter((key) => !expanded.has(key));
    const differenceToRemove = [...expanded].filter((key) => !newKeys.has(key));

    const updatedExpanded = new Set(expanded);
    differenceToAdd.forEach(async (key) => {
      if (!loadedPaths.has(key)) {
        setLoadedPaths(prevPaths => new Set(prevPaths).add(key));
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
    if (rootPath == '/') {
      rootPath = "";
    }
    // Expand the root path when the component mounts
    const initializePathInput = async () => {
      if (!loadedPaths.has(rootPath)) {
        setLoadedPaths(prevPaths => new Set(prevPaths).add(rootPath));
        await loadSuggestions(rootPath);
      }
      const expandPathToValue = async (path: string) => {
        const pathParts = path.split("/").filter(Boolean);
        let currentPath = "";
        const tempExpanded = new Set<string>([rootPath]);
        const tempLoadedPaths = new Set(loadedPaths);
        // Expand each part of the path except for the last oen
        for (const part of pathParts.slice(0, -1)) {
          currentPath = `${currentPath}/${part}`.replace(/\/+/g, '/');
          if (!tempLoadedPaths.has(currentPath)) {
            await loadSuggestions(currentPath);
            tempLoadedPaths.add(currentPath);
            tempExpanded.add(currentPath);
          }
        }

        setLoadedPaths(new Set(tempLoadedPaths));
        setExpanded(new Set(tempExpanded));
      };
      await expandPathToValue(value);
      setSelected(new Set([value]));
    };
    if (value != null && value.length > 0) {
      // Offload the initialization to avoid blocking the UI
      setTimeout(async () => {
        await initializePathInput()
      }, 0)
    }
  }, [rootPath, loadSuggestions]);

  return (
    <>
      <p className={styles.label}>{label}</p>
      <div ref={ref} tabIndex={-1} style={{outline: "none", width: "100%", height: "100%"}}>
        <TreeView {...props} selectionMode={"single"} width={'100%'} onSelectionChange={onSelectionChange} onExpandedChange={onExpandedChange} defaultSelectedKeys={new Set(value)} selectedKeys={selected} expandedKeys={expanded}>
          {items && <TreeItem node={items} path={''} />}
        </TreeView>
      </div>
    </>
  );
})

const TreeItem = ({ node, path }: { node: Node; path: string }) => {
  return (
    <TreeViewItem id={path + node.name} textValue={node.name.length > 0 ? node.name : "/"}>
      <TreeViewItemContent>
        <Text>{node.name.length > 0 ? node.name.replace("/", "") : '/'}</Text>
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
