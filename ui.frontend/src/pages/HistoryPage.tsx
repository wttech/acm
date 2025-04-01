import { Cell, Column, Content, DatePicker, Flex, IllustratedMessage, Item, NumberField, Picker, ProgressBar, Row, TableBody, TableHeader, TableView, Text, TextField, View } from '@adobe/react-spectrum';
import { CalendarDateTime, DateValue } from '@internationalized/date';
import { Key } from '@react-types/shared';
import NotFound from '@spectrum-icons/illustrations/NotFound';
import Alert from '@spectrum-icons/workflow/Alert';
import Cancel from '@spectrum-icons/workflow/Cancel';
import Checkmark from '@spectrum-icons/workflow/Checkmark';
import Pause from '@spectrum-icons/workflow/Pause';
import Search from '@spectrum-icons/workflow/Search';
import Star from '@spectrum-icons/workflow/Star';
import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useDebounce } from 'react-use';
import DateExplained from '../components/DateExplained';
import ExecutableValue from '../components/ExecutableValue.tsx';
import ExecutionStatusBadge from '../components/ExecutionStatusBadge.tsx';
import { searchParams } from '../constants/searchParams.ts';
import { useFormatter } from '../hooks/formatter';
import { toastRequest } from '../utils/api';
import { ExecutionOutput, ExecutionStatus } from '../utils/api.types';

const getDateFromStringValue = (urlValue: string | null, fallback?: CalendarDateTime) => {
  if (urlValue) {
    const date = new Date(urlValue);

    return new CalendarDateTime(date.getFullYear(), date.getMonth() + 1, date.getDate(), date.getHours(), date.getMinutes(), date.getSeconds());
  }

  return fallback ?? null;
};

const HistoryPage = () => {
  const navigate = useNavigate();
  const [executions, setExecutions] = useState<ExecutionOutput | null>(null);
  const [loading, setLoading] = useState<boolean>(false);

  const sevenDaysAgo = new Date(); // assume start date to be 7 days ago
  sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7);
  const startDateDefault = new CalendarDateTime(sevenDaysAgo.getFullYear(), sevenDaysAgo.getMonth() + 1, sevenDaysAgo.getDate(), 0, 0, 0);

  const [searchState, setSearchState] = useSearchParams();
  const [startDate, setStartDate] = useState<DateValue | null>(getDateFromStringValue(searchState.get(searchParams.startDate), startDateDefault));
  const [endDate, setEndDate] = useState<DateValue | null>(getDateFromStringValue(searchState.get(searchParams.endDate)));
  const [status, setStatus] = useState<string | null>(searchState.get(searchParams.status) || 'all');
  const [executableId, setExecutableId] = useState<string>(searchState.get(searchParams.executableId) || '');

  const [durationMinFromParam, durationMaxFromParam] = (() => {
    const urlValue = searchState.get(searchParams.duration);

    if (!urlValue) {
      return [undefined, undefined];
    }

    const [min, max] = urlValue.split(',');

    return [Number(min), Number(max)];
  })();

  const [durationMin, setDurationMin] = useState<number | undefined>(durationMinFromParam);
  const [durationMax, setDurationMax] = useState<number | undefined>(durationMaxFromParam);

  const formatter = useFormatter();

  useDebounce(
    () => {
      const fetchExecutions = async () => {
        setLoading(true);
        try {
          let url = `/apps/acm/api/execution.json`;

          const params = new URLSearchParams();

          if (executableId) params.append(searchParams.executableId, `%${executableId}%`);
          if (startDate) params.append(searchParams.startDate, startDate.toString());
          if (endDate) params.append(searchParams.endDate, endDate.toString());
          if (status && status !== 'all') params.append(searchParams.status, status);
          if (durationMin || durationMax) params.append(searchParams.duration, `${durationMin || ''},${durationMax || ''}`);
          if (params.toString()) url += `?${params.toString()}`;

          setSearchState(params.toString(), { replace: true });

          const response = await toastRequest<ExecutionOutput>({
            method: 'GET',
            url,
            operation: `Executions loading`,
            positive: false,
          });
          setExecutions(response.data.data);
        } catch (error) {
          console.error('Cannot load executions:', error);
        } finally {
          setLoading(false);
        }
      };
      fetchExecutions();
    },
    500,
    [startDate, endDate, status, executableId, durationMin, durationMax],
  );

  const renderEmptyState = () => (
    <IllustratedMessage>
      <NotFound />
      <Content>No executions found</Content>
    </IllustratedMessage>
  );

  return (
    <Flex direction="column" flex="1" gap="size-200" marginY="size-100">
      <View borderBottomWidth="thick" borderColor="gray-300" paddingBottom="size-200" marginBottom="size-10">
        <Flex direction="row" gap="size-200" alignItems="center" justifyContent="space-around">
          <TextField icon={<Search />} label="Executable" value={executableId} type="search" onChange={setExecutableId} placeholder="e.g. script name" />
          <DatePicker label="Start Date" granularity="second" value={startDate} onChange={setStartDate} />
          <DatePicker label="End Date" granularity="second" value={endDate} onChange={setEndDate} />
          <NumberField label="Min Duration (ms)" value={durationMin} onChange={setDurationMin} />
          <NumberField label="Max Duration (ms)" value={durationMax} onChange={setDurationMax} />
          <Picker label="Status" selectedKey={status} onSelectionChange={(key) => setStatus(String(key))}>
            <Item textValue="All" key="all">
              <Star size="S" />
              <Text>All</Text>
            </Item>
            <Item textValue="Skipped" key={ExecutionStatus.SKIPPED}>
              <Pause size="S" />
              <Text>Skipped</Text>
            </Item>
            <Item textValue="Aborted" key={ExecutionStatus.ABORTED}>
              <Cancel size="S" />
              <Text>Aborted</Text>
            </Item>
            <Item textValue="Failed" key={ExecutionStatus.FAILED}>
              <Alert size="S" />
              <Text>Failed</Text>
            </Item>
            <Item textValue="Succeeded" key={ExecutionStatus.SUCCEEDED}>
              <Checkmark size="S" />
              <Text>Succeeded</Text>
            </Item>
          </Picker>
        </Flex>
      </View>
      {executions === null || loading ? (
        <Flex flex="1" justifyContent="center" alignItems="center" height="100vh">
          <ProgressBar label="Loading..." isIndeterminate />
        </Flex>
      ) : (
        <TableView flex="1" aria-label="Executions table" selectionMode="none" renderEmptyState={renderEmptyState} onAction={(key: Key) => navigate(`/executions/view/${encodeURIComponent(key)}`)}>
          <TableHeader>
            <Column>Executable</Column>
            <Column>Started</Column>
            <Column>Duration</Column>
            <Column>Status</Column>
          </TableHeader>
          <TableBody>
            {(executions?.list || []).map((execution) => (
              <Row key={execution.id}>
                <Cell>
                  <ExecutableValue value={execution.executable} />
                </Cell>
                <Cell>
                  <DateExplained value={execution.startDate} />
                </Cell>
                <Cell>{formatter.duration(execution.duration)}</Cell>
                <Cell>
                  <ExecutionStatusBadge value={execution.status} />
                </Cell>
              </Row>
            ))}
          </TableBody>
        </TableView>
      )}
    </Flex>
  );
};

export default HistoryPage;
