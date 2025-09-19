import { Badge, Button, ButtonGroup, Cell, Column, Content, Dialog, DialogTrigger, Divider, Flex, Heading, IllustratedMessage, Row, StatusLight, TableBody, TableHeader, TableView, Text, View } from '@adobe/react-spectrum';
import NoSearchResults from '@spectrum-icons/illustrations/NoSearchResults';
import Checkmark from '@spectrum-icons/workflow/Checkmark';
import Close from '@spectrum-icons/workflow/Close';
import Code from '@spectrum-icons/workflow/Code';
import Data from '@spectrum-icons/workflow/Data';
import Gears from '@spectrum-icons/workflow/Gears';
import Help from '@spectrum-icons/workflow/Help';
import Magnify from '@spectrum-icons/workflow/Magnify';
import Question from '@spectrum-icons/workflow/Question';
import Replay from '@spectrum-icons/workflow/Replay';
import Settings from '@spectrum-icons/workflow/Settings';
import UploadToCloud from '@spectrum-icons/workflow/UploadToCloud';
import { ReactNode } from 'react';
import { useAppState } from '../hooks/app.ts';
import { HealthIssueSeverity, instancePrefix, InstanceType } from '../utils/api.types';
import { Strings } from '../utils/strings.ts';
import CodeEditor from './CodeEditor';

const HealthChecker = () => {
  const appState = useAppState();
  const healthIssues = appState.healthStatus.issues;

  const getSeverityVariant = (severity: HealthIssueSeverity): 'negative' | 'yellow' | 'neutral' => {
    switch (severity) {
      case HealthIssueSeverity.CRITICAL:
        return 'negative';
      case HealthIssueSeverity.WARNING:
        return 'yellow';
      default:
        return 'neutral';
    }
  };

  const getCategoryFragment = (category: string): ReactNode => {
    switch (category) {
      case 'INSTANCE':
        return (
          <>
            <Settings size="S" />
            Instance
          </>
        );
      case 'REPOSITORY':
        return (
          <>
            <Data size="S" />
            Repository
          </>
        );
      case 'OSGI':
        return (
          <>
            <Gears size="S" />
            OSGi
          </>
        );
      case 'INSTALLER':
        return (
          <>
            <UploadToCloud size="S" />
            Installer
          </>
        );
      case 'CODE_EXECUTOR':
        return (
          <>
            <Code size="S" />
            Code executor
          </>
        );
      default:
        return (
          <>
            <Question size="S" />
            Other
          </>
        );
    }
  };

  const renderEmptyState = () => (
    <IllustratedMessage>
      <NoSearchResults />
      <Content>No issues</Content>
    </IllustratedMessage>
  );

  return (
    <Flex direction="column" flex="1" gap="size-200" marginY="size-100">
      <View>
        <Flex direction="row" justifyContent="space-between" alignItems="center">
          <Flex flex="1" alignItems="center">
            &nbsp;
          </Flex>
          <Flex flex="1" justifyContent="center" alignItems="center">
            <StatusLight variant={healthIssues.length === 0 ? 'positive' : 'negative'}>{healthIssues.length === 0 ? <>Healthy</> : <>Unhealthy &mdash; {healthIssues.length} issue(s)</>}</StatusLight>
          </Flex>
          <Flex flex="1" justifyContent="end" alignItems="center">
            <ButtonGroup>
              <Button
                variant="negative"
                isDisabled={appState.instanceSettings.type === InstanceType.CLOUD_CONTAINER}
                onPress={() => window.open(`${instancePrefix}/system/console/configMgr/dev.vml.es.acm.core.instance.HealthChecker`, '_blank')}
              >
                <Settings />
                <Text>Configure</Text>
              </Button>
              <DialogTrigger>
                <Button variant="secondary" style="fill">
                  <Help />
                  <Text>Help</Text>
                </Button>
                {(close) => (
                  <Dialog>
                    <Heading>Health Checker</Heading>
                    <Divider />
                    <Content>
                      <p>
                        <Help size="XS" /> All detected health issues will be comprehensively listed here for your review.
                      </p>
                      <p>
                        <Replay size="XS" /> If needed, configure which OSGi bundles should be ignored when determining the healthy state of the instance.
                      </p>
                      <p>
                        <Checkmark size="XS" /> Additionally, you can configure OSGi event topics to be checked within a recent time window.
                      </p>
                    </Content>
                    <ButtonGroup>
                      <Button variant="secondary" onPress={close}>
                        <Close size="XS" />
                        <Text>Close</Text>
                      </Button>
                    </ButtonGroup>
                  </Dialog>
                )}
              </DialogTrigger>
            </ButtonGroup>
          </Flex>
        </Flex>
      </View>

      <TableView flex="1" aria-label="Health Issues" renderEmptyState={renderEmptyState} selectionMode="none" marginY="size-200" minHeight="size-3400">
        <TableHeader>
          <Column width="1fr">#</Column>
          <Column width="2fr">Severity</Column>
          <Column width="3fr">Category</Column>
          <Column width="8fr">Issue</Column>
          <Column width="2fr">Details</Column>
        </TableHeader>
        <TableBody>
          {(healthIssues || []).map((issue, index) => (
            <Row key={index}>
              <Cell>{index + 1}</Cell>
              <Cell>
                <Badge variant={getSeverityVariant(issue.severity)}>{issue.severity}</Badge>
              </Cell>
              <Cell>
                <Flex alignItems="center" gap="size-100">
                  {getCategoryFragment(issue.category)}
                </Flex>
              </Cell>
              <Cell>
                <Text>{issue.issue}</Text>
              </Cell>
              <Cell>
                {Strings.notBlank(issue.details) ? (
                  <DialogTrigger>
                    <Button variant="secondary">
                      <Magnify />
                      <Text>View</Text>
                    </Button>
                    {(close) => (
                      <Dialog width="75vw">
                        <Heading>{issue.issue}</Heading>
                        <Divider />
                        <Content>
                          <View height="size-4600">
                            <CodeEditor id={`health-issue-${index}`} language="text" readOnly value={issue.details || 'No additional details available.'} />
                          </View>
                        </Content>
                        <ButtonGroup>
                          <Button variant="secondary" onPress={close}>
                            <Close size="XS" />
                            <Text>Close</Text>
                          </Button>
                        </ButtonGroup>
                      </Dialog>
                    )}
                  </DialogTrigger>
                ) : (
                  <Text>&mdash;</Text>
                )}
              </Cell>
            </Row>
          ))}
        </TableBody>
      </TableView>
    </Flex>
  );
};

export default HealthChecker;
