import { Button, ButtonGroup, Cell, Column, Content, Flex, IllustratedMessage, ProgressBar, Row, StatusLight, TableBody, TableHeader, TableView, Text, View } from '@adobe/react-spectrum';
import NotFound from '@spectrum-icons/illustrations/NotFound';
import Settings from '@spectrum-icons/workflow/Settings';
import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAppState } from '../hooks/app.ts';
import { toastRequest } from '../utils/api';
import { InstanceType, ScriptOutput, ScriptType, instancePrefix } from '../utils/api.types';
import ScriptsAutomaticHelpButton from './ScriptsAutomaticHelpButton.tsx';
import ScriptsManualHelpButton from './ScriptsManualHelpButton.tsx';

type ScriptListSimpleProps = {
  type: ScriptType;
};

const ScriptListSimple: React.FC<ScriptListSimpleProps> = ({ type }) => {
  const [scripts, setScripts] = useState<ScriptOutput | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const navigate = useNavigate();
  const appState = useAppState();

  const loadScripts = useCallback(() => {
    setLoading(true);
    toastRequest<ScriptOutput>({
      method: 'GET',
      url: `/apps/acm/api/script.json?type=${type}`,
      operation: `Scripts loading (${type.toString().toLowerCase()})`,
      positive: false,
    })
      .then((data) => setScripts(data.data.data))
      .catch((error) => console.error(`Scripts loading (${type}) error:`, error))
      .finally(() => setLoading(false));
  }, [type]);

  useEffect(() => {
    loadScripts();
  }, [type, loadScripts]);

  const renderEmptyState = () => (
    <IllustratedMessage>
      <NotFound />
      <Content>No scripts found</Content>
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
            <ButtonGroup>
              {type === ScriptType.MOCK ? (
                <Button
                  variant="negative"
                  isDisabled={appState.instanceSettings.type === InstanceType.CLOUD_CONTAINER}
                  onPress={() => window.open(`${instancePrefix}/system/console/configMgr/com.vml.es.aem.acm.core.mock.MockHttpFilter`, '_blank')}
                >
                  <Settings />
                  <Text>Configure</Text>
                </Button>
              ) : null}
            </ButtonGroup>
          </Flex>
          <Flex flex="1" justifyContent="center" alignItems="center">
            <StatusLight variant={appState.healthStatus.healthy ? 'positive' : 'negative'}>{appState.mockStatus.enabled ? <Text>Mocks enabled</Text> : <Text>Mocks disabled</Text>}</StatusLight>
          </Flex>
          <Flex flex="1" justifyContent="end" alignItems="center">
            {type === ScriptType.MANUAL ? <ScriptsManualHelpButton /> : <ScriptsAutomaticHelpButton />}
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

export default ScriptListSimple;
