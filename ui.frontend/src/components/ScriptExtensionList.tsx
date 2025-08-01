import { Cell, Column, Content, Flex, IllustratedMessage, ProgressBar, Row, StatusLight, TableBody, TableHeader, TableView, Text, View } from '@adobe/react-spectrum';
import NotFound from '@spectrum-icons/illustrations/NotFound';
import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useScripts } from '../hooks/script';
import { ScriptType } from '../utils/api.types';
import ScriptsExtensionHelpButton from './ScriptsExtensionHelpButton.tsx';

const ScriptExtensionList: React.FC = () => {
  const type = ScriptType.EXTENSION;
  const { scripts, loading } = useScripts(type);
  const scriptCount = scripts?.list?.length ?? 0;
  const navigate = useNavigate();

  const renderEmptyState = () => (
    <IllustratedMessage>
      <NotFound />
      <Content>No extensions found</Content>
    </IllustratedMessage>
  );

  if (scripts === null || loading) {
    return (
      <Flex flex="1" justifyContent="center" alignItems="center">
        <ProgressBar label="Loading..." isIndeterminate />
      </Flex>
    );
  }

  return (
    <Flex direction="column" flex="1" gap="size-200" marginY="size-100">
      <View>
        <Flex direction="row" justifyContent="space-between" alignItems="center">
          <Flex flex="1" alignItems="center">
            &nbsp;
          </Flex>
          <Flex flex="1" justifyContent="center" alignItems="center">
            <StatusLight variant={scriptCount > 0 ? 'positive' : 'negative'}>{scriptCount > 0 ? <Text>Installed ({scriptCount})</Text> : <Text>Not installed</Text>}</StatusLight>
          </Flex>
          <Flex flex="1" justifyContent="end" alignItems="center">
            <ScriptsExtensionHelpButton />
          </Flex>
        </Flex>
      </View>
      <TableView flex="1" aria-label={`Script list (${type})`} renderEmptyState={renderEmptyState} onAction={(key) => navigate(`/scripts/view/${encodeURIComponent(key)}`)}>
        <TableHeader>
          <Column>Name</Column>
        </TableHeader>
        <TableBody>
          {(scripts.list || []).map((script) => (
            <Row key={script.id}>
              <Cell>{script.name}</Cell>
            </Row>
          ))}
        </TableBody>
      </TableView>
    </Flex>
  );
};

export default ScriptExtensionList;
