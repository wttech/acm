import { Button, ButtonGroup, Content, Flex, IllustratedMessage, Item, LabeledValue, ProgressBar, TabList, TabPanels, Tabs, Text, View } from '@adobe/react-spectrum';
import { Field } from '@react-spectrum/label';
import { ToastQueue } from '@react-spectrum/toast';
import NotFound from '@spectrum-icons/illustrations/NotFound';
import Copy from '@spectrum-icons/workflow/Copy';
import FileCode from '@spectrum-icons/workflow/FileCode';
import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import ImmersiveEditor from '../components/ImmersiveEditor.tsx';
import { toastRequest } from '../utils/api';
import { Script, ScriptOutput } from '../utils/api.types';

const toastTimeout = 3000;

const ScriptView = () => {
  const [script, setScript] = useState<Script | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const scriptId = decodeURIComponent(useParams<{ scriptId: string }>().scriptId as string);

  useEffect(() => {
    const fetchScript = async () => {
      setLoading(true);
      try {
        const response = await toastRequest<ScriptOutput>({
          method: 'GET',
          url: `/apps/acm/api/script.json?id=${scriptId}`,
          operation: `Script loading ${scriptId}`,
          positive: false,
        });
        setScript(response.data.data.list[0]);
      } catch (error) {
        console.error(`Script cannot be loaded '${scriptId}':`, error);
      } finally {
        setLoading(false);
      }
    };
    fetchScript();
  }, [scriptId]);

  if (loading) {
    return (
      <Flex flex="1" justifyContent="center" alignItems="center">
        <ProgressBar label="Loading..." isIndeterminate />
      </Flex>
    );
  }

  if (!script) {
    return (
      <Flex direction="column" flex="1">
        <IllustratedMessage>
          <NotFound />
          <Content>Script not found</Content>
        </IllustratedMessage>
      </Flex>
    );
  }

  const onCopyScriptCode = () => {
    navigator.clipboard
      .writeText(script.content)
      .then(() => {
        ToastQueue.info('Script code copied to clipboard!', {
          timeout: toastTimeout,
        });
      })
      .catch(() => {
        ToastQueue.negative('Failed to copy script code!', {
          timeout: toastTimeout,
        });
      });
  };

  return (
    <Flex direction="column" flex="1" gap="size-400">
      <Tabs flex="1" aria-label="Script Details">
        <TabList>
          <Item key="details">
            <FileCode />
            <Text>Script</Text>
          </Item>
          <Item key="code" aria-label="Code">
            <FileCode />
            <Text>Code</Text>
          </Item>
        </TabList>
        <TabPanels flex="1" UNSAFE_style={{ display: 'flex' }}>
          <Item key="details">
            <Flex direction="column" flex="1" gap="size-200" marginY="size-100">
              <View backgroundColor="gray-50" padding="size-200" borderRadius="medium" borderColor="dark" borderWidth="thin">
                <Flex direction="row" justifyContent="space-between" gap="size-200">
                  <Field label="Name">
                    <div>
                      <Text>{script.name}</Text>
                    </div>
                  </Field>
                  <LabeledValue label="ID" value={script.id} />
                </Flex>
              </View>
            </Flex>
          </Item>
          <Item key="code">
            <Flex direction="column" flex="1" gap="size-200" marginY="size-100">
              <View>
                <Flex justifyContent="space-between" alignItems="center">
                  <ButtonGroup>
                    <Button variant="secondary" onPress={onCopyScriptCode}>
                      <Copy />
                      <Text>Copy</Text>
                    </Button>
                  </ButtonGroup>
                </Flex>
              </View>
              <ImmersiveEditor id="script-view" value={script.content} language="groovy" readOnly />
            </Flex>
          </Item>
        </TabPanels>
      </Tabs>
    </Flex>
  );
};

export default ScriptView;
