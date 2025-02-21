import {
  Button,
  ButtonGroup,
  Cell,
  Column,
  Content,
  Dialog,
  DialogTrigger,
  Divider,
  Flex,
  Heading,
  IllustratedMessage,
  ContextualHelp,
  ProgressBar,
  Row,
  TableBody,
  TableHeader,
  TableView,
  Text,
  View,
  ActionButton
} from '@adobe/react-spectrum';
import { Key, Selection } from '@react-types/shared';
import NotFound from '@spectrum-icons/illustrations/NotFound';
import Cancel from '@spectrum-icons/workflow/Cancel';
import Checkmark from '@spectrum-icons/workflow/Checkmark';
import PlayCircle from '@spectrum-icons/workflow/PlayCircle';
import React, { useCallback, useEffect, useState } from 'react';
import { toastRequest } from '../utils/api';
import { ScriptOutput } from '../utils/api.types';
import { useFormatter } from '../utils/hooks.ts';
import ExecutionStatsBadge from './ExecutionStatsBadge';
import {useNavigate} from "react-router-dom";
import {AppLink} from "../AppLink.tsx";
import ArrowRight from "@spectrum-icons/workflow/ArrowRight";
import FullScreenExit from "@spectrum-icons/workflow/FullScreenExit";
import Magnify from "@spectrum-icons/workflow/Magnify";

type ScriptListProps = {
  type: 'enabled' | 'disabled';
};

const ScriptList: React.FC<ScriptListProps> = ({ type }) => {
  const navigate = useNavigate();
  const formatter = useFormatter();

  const [scripts, setScripts] = useState<ScriptOutput | null>(null);
  const [selectedKeys, setSelectedKeys] = useState<Selection>(new Set<Key>());
  const [toggleDialogOpen, setToggleDialogOpen] = useState(false);

  const loadScripts = useCallback(() => {
    toastRequest<ScriptOutput>({
      method: 'GET',
      url: `/apps/contentor/api/script.json?type=${type}`,
      operation: `Scripts loading (${type})`,
      positive: false,
    })
      .then((data) => setScripts(data.data.data))
      .catch((error) => console.error(`Scripts loading (${type}) error:`, error));
  }, [type]);

  useEffect(() => {
    loadScripts();
  }, [type, loadScripts]);

  const selectedIds = (selectedKeys: Selection): string[] => {
    if (selectedKeys === 'all') {
      return scripts?.list.map((script) => script.id) || [];
    } else {
      return Array.from(selectedKeys as Set<Key>).map((key) => key.toString());
    }
  };

  const handleConfirm = async () => {
    const action = type === 'enabled' ? 'disable' : 'enable';
    const ids = selectedIds(selectedKeys);

    const params = new URLSearchParams();
    params.append('action', action);
    params.append('type', type);
    ids.forEach((id) => params.append('id', id));

    try {
      await toastRequest({
        method: 'POST',
        url: `/apps/contentor/api/script.json?${params.toString()}`,
        operation: `${action} scripts`,
      });
      loadScripts();
      setSelectedKeys(new Set<Key>());
    } catch (error) {
      console.error(`${action} scripts error:`, error);
    } finally {
      setToggleDialogOpen(false);
    }
  };

  const renderEmptyState = () => (
    <IllustratedMessage>
      <NotFound />
      <Content>No scripts found</Content>
    </IllustratedMessage>
  );

  const renderToggleDialog = () => (
    <>
      <Heading>
        <Text>Confirmation</Text>
      </Heading>
      <Divider />
      <Content>
        {type === 'enabled' ? (
          <Text>Disabling scripts will stop their automatic execution. To execute them again, you need to enable them or reinstall the package with scripts.</Text>
        ) : (
          <Text>Enabling scripts can cause changes in the repository and potential data loss. Ensure the script is ready to use. It is recommended to provide enabled scripts via a package, not manually.</Text>
        )}
      </Content>
      <ButtonGroup>
        <Button variant="secondary" onPress={() => setToggleDialogOpen(false)}>
          <Cancel />
          <Text>Cancel</Text>
        </Button>
        <Button variant="negative" style="fill" onPress={handleConfirm}>
          <Checkmark />
          <Text>Confirm</Text>
        </Button>
      </ButtonGroup>
    </>
  );

  if (scripts === null) {
    return (
      <Flex flex="1" justifyContent="center" alignItems="center">
        <ProgressBar label="Loading..." isIndeterminate />
      </Flex>
    );
  }

  return (
    <Flex direction="column" flex="1" gap="size-200" marginY="size-100">
      <View>
        <Flex justifyContent="space-between" alignItems="center">
          <ButtonGroup>
            <DialogTrigger isOpen={toggleDialogOpen} onOpenChange={setToggleDialogOpen}>
              <Button variant={type === 'enabled' ? 'negative' : 'accent'} style="fill" isDisabled={selectedKeys === 'all' ? false : (selectedKeys as Set<Key>).size === 0} onPress={() => setToggleDialogOpen(true)}>
                {type === 'enabled' ? <Cancel /> : <PlayCircle />}
                <Text>{type === 'enabled' ? 'Disable' : 'Enable'}</Text>
              </Button>
              <Dialog>{renderToggleDialog()}</Dialog>
            </DialogTrigger>
          </ButtonGroup>
        </Flex>
      </View>
      <TableView flex="1"
                 aria-label="Scripts list"
                 selectionMode="multiple"
                 selectedKeys={selectedKeys}
                 onSelectionChange={setSelectedKeys}
                 renderEmptyState={renderEmptyState}
                 onAction={(key) => {navigate(`/scripts/view/${encodeURIComponent(key)}`)}}
      >
        <TableHeader>
          <Column>Name</Column>
          <Column>Last Execution</Column>
          <Column>
            Success Rate
            <ContextualHelp variant="help">
              <Content>Success rate is calculated based on the last 30 completed executions (only succeeded or failed).</Content>
            </ContextualHelp>
          </Column>
        </TableHeader>
        <TableBody>
          {(scripts.list || []).map((script) => {
            const scriptStats = (scripts.stats.find((stat) => stat.path === script.id))!;
            const lastExecution = scriptStats?.lastExecution;

            return (
              <Row key={script.id}>
                <Cell>{script.name}</Cell>
                <Cell>
                    {lastExecution ? (
                        <>
                          <Text>{formatter.dateExplained(lastExecution.startDate)}</Text>
                          &nbsp;
                          <AppLink to={`/executions/view/${encodeURIComponent(lastExecution?.id)}`}>
                            &rArr;
                          </AppLink>
                        </>
                    ) : <>&mdash;</>}
                </Cell>
                <Cell><ExecutionStatsBadge stats={scriptStats} /></Cell>
              </Row>
            );
          })}
        </TableBody>
      </TableView>
    </Flex>
  );
};

export default ScriptList;
