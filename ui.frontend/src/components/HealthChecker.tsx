import { Badge, Button, ButtonGroup, Cell, Column, Content, Dialog, DialogTrigger, Divider, Flex, Heading, IllustratedMessage, Row, StatusLight, TableBody, TableHeader, TableView, Text, View } from '@adobe/react-spectrum';
import NoSearchResults from '@spectrum-icons/illustrations/NoSearchResults';
import Checkmark from '@spectrum-icons/workflow/Checkmark';
import Close from '@spectrum-icons/workflow/Close';
import Help from '@spectrum-icons/workflow/Help';
import Replay from '@spectrum-icons/workflow/Replay';
import Settings from '@spectrum-icons/workflow/Settings';
import { useAppState } from '../hooks/app.ts';
import { HealthIssueSeverity, instancePrefix, InstanceType } from '../utils/api.types';

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
            <Button
              variant="negative"
              isDisabled={appState.instanceSettings.type === InstanceType.CLOUD_CONTAINER}
              onPress={() => window.open(`${instancePrefix}/system/console/configMgr/dev.vml.es.acm.core.instance.HealthChecker`, '_blank')}
            >
              <Settings />
              <Text>Configure</Text>
            </Button>
          </Flex>
          <Flex flex="1" justifyContent="center" alignItems="center">
            <StatusLight variant={healthIssues.length === 0 ? 'positive' : 'negative'}>{healthIssues.length === 0 ? <>Healthy</> : <>Unhealthy &mdash; {healthIssues.length} issue(s)</>}</StatusLight>
          </Flex>
          <Flex flex="1" justifyContent="end" alignItems="center">
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
          </Flex>
        </Flex>
      </View>

      <TableView flex="1" aria-label="Health Issues" renderEmptyState={renderEmptyState} selectionMode="none" marginY="size-200" minHeight="size-3400">
        <TableHeader>
          <Column width="5%">#</Column>
          <Column>Severity</Column>
          <Column>Message</Column>
        </TableHeader>
        <TableBody>
          {(healthIssues || []).map((issue, index) => (
            <Row key={index}>
              <Cell>{index + 1}</Cell>
              <Cell>
                <Badge variant={getSeverityVariant(issue.severity)}>{issue.severity}</Badge>
              </Cell>
              <Cell>{issue.message}</Cell>
            </Row>
          ))}
        </TableBody>
      </TableView>
    </Flex>
  );
};

export default HealthChecker;
