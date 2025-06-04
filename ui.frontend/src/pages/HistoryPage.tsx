import { Cell, Column, Content, DatePicker, Flex, IllustratedMessage, Item, NumberField, Picker, ProgressBar, Row, TableBody, TableHeader, TableView, Text, TextField, View } from '@adobe/react-spectrum';
import { DateValue } from '@internationalized/date';
import { Key } from '@react-types/shared';
import NotFound from '@spectrum-icons/illustrations/NotFound';
import Alert from '@spectrum-icons/workflow/Alert';
import Cancel from '@spectrum-icons/workflow/Cancel';
import Checkmark from '@spectrum-icons/workflow/Checkmark';
import Pause from '@spectrum-icons/workflow/Pause';
import Search from '@spectrum-icons/workflow/Search';
import Star from '@spectrum-icons/workflow/Star';
import User from '@spectrum-icons/workflow/User';
import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useDebounce } from 'react-use';
import DateExplained from '../components/DateExplained';
import ExecutableIdValue from '../components/ExecutableIdValue';
import ExecutionStatusBadge from '../components/ExecutionStatusBadge';
import UserInfo from '../components/UserInfo';
import { useFormatter } from '../hooks/formatter';
import { toastRequest } from '../utils/api';
import { ExecutionFormat, ExecutionOutput, ExecutionQueryParams, ExecutionStatus, ExecutionSummary, isExecutableExplicit } from '../utils/api.types';
import { Dates } from '../utils/dates';
import { Urls } from '../utils/url';
import { useDebouncedState } from '@mantine/hooks';

const HistoryFetchDelay = 1000;
const HistoryFilterDelay = 1000;

const HistoryPage = () => {
  const navigate = useNavigate();
  const [executions, setExecutions] = useState<ExecutionOutput<ExecutionSummary> | null>(null);
  const [loading, setLoading] = useState<boolean>(false);

  const [searchState, setSearchState] = useSearchParams();
  const [startDate, setStartDate] = useDebouncedState<DateValue | null>(Dates.toCalendarDateTimeOrNull(searchState.get(ExecutionQueryParams.START_DATE)) ?? Dates.toCalendarDateTime(Dates.daysAgoAtMidnight(7)), HistoryFilterDelay);
  const [endDate, setEndDate] = useDebouncedState<DateValue | null>(Dates.toCalendarDateTimeOrNull(searchState.get(ExecutionQueryParams.END_DATE)), HistoryFilterDelay);
  const [status, setStatus] = useDebouncedState<string | null>(searchState.get(ExecutionQueryParams.STATUS) || 'all', HistoryFilterDelay);
  const [executableId, setExecutableId] = useDebouncedState<string>(searchState.get(ExecutionQueryParams.EXECUTABLE_ID) || '', HistoryFilterDelay);
  const [userId, setUserId] = useDebouncedState<string>(searchState.get(ExecutionQueryParams.USER_ID) || '', HistoryFilterDelay);

  const [durationMinInitial, durationMaxInitial] = (() => {
    const searchParam = searchState.get(ExecutionQueryParams.DURATION);
    if (!searchParam) return [undefined, undefined];
    const [min, max] = searchParam.split(',');
    return [Number(min), Number(max)];
  })();
  const [durationMin, setDurationMin] = useDebouncedState<number | undefined>(durationMinInitial, HistoryFilterDelay);
  const [durationMax, setDurationMax] = useDebouncedState<number | undefined>(durationMaxInitial, HistoryFilterDelay);

  const formatter = useFormatter();

  useDebounce(
      () => {
        const fetchExecutions = async () => {
          setLoading(true);
          try {
            const params = new URLSearchParams();
            params.append(ExecutionQueryParams.FORMAT, ExecutionFormat.SUMMARY);
            if (executableId) params.append(ExecutionQueryParams.EXECUTABLE_ID, executableId);
            if (userId) params.append(ExecutionQueryParams.USER_ID, userId);
            if (startDate) params.append(ExecutionQueryParams.START_DATE, startDate.toString());
            if (endDate) params.append(ExecutionQueryParams.END_DATE, endDate.toString());
            if (status && status !== 'all') params.append(ExecutionQueryParams.STATUS, status);
            if (durationMin || durationMax) params.append(ExecutionQueryParams.DURATION, `${durationMin || ''},${durationMax || ''}`);

            setSearchState(params.toString(), {replace: true});

            const response = await toastRequest<ExecutionOutput<ExecutionSummary>>({
              method: 'GET',
              url: Urls.compose('/apps/acm/api/execution.json',  (() => {
                const result = new URLSearchParams(params);
                const wildcard = '@W@';
                result.delete(ExecutionQueryParams.EXECUTABLE_ID);
                result.append(ExecutionQueryParams.EXECUTABLE_ID, isExecutableExplicit(executableId) ? executableId : `${wildcard}${executableId}${wildcard}`);
                result.delete(ExecutionQueryParams.USER_ID);
                result.append(ExecutionQueryParams.USER_ID, `${wildcard}${userId}${wildcard}`);
                return result;
              })()),
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
      HistoryFetchDelay,
      [startDate, endDate, status, executableId, userId, durationMin, durationMax],
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
        <Flex direction="row" gap="size-200" alignItems="center">
          <TextField flex="1" icon={<Search />} label="Executable" value={executableId} type="search" onChange={setExecutableId} placeholder="e.g. script name" />
          <TextField flex="1" icon={<User />} label="User" value={userId} type="search" onChange={setUserId} placeholder="ID" />
          <DatePicker width="size-3000" label="Start Date" granularity="second" value={startDate} onChange={setStartDate} />
          <DatePicker width="size-3000" label="End Date" granularity="second" value={endDate} onChange={setEndDate} />
          <NumberField width="size-1600" label="Min Duration (ms)" value={durationMin} onChange={setDurationMin} />
          <NumberField width="size-1600" label="Max Duration (ms)" value={durationMax} onChange={setDurationMax} />
          <Picker width="size-2000" label="Status" selectedKey={status} onSelectionChange={(key) => setStatus(String(key))}>
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
            <Column width="6fr">Executable</Column>
            <Column width="3fr">User</Column>
            <Column width="4fr">Started</Column>
            <Column width="3fr">Duration</Column>
            <Column width="2fr" align="end">
              Status
            </Column>
          </TableHeader>
          <TableBody>
            {(executions?.list || []).map((execution) => (
              <Row key={execution.id}>
                <Cell>
                  <ExecutableIdValue id={execution.executableId} />
                </Cell>
                <Cell>
                  <UserInfo id={execution.userId} />
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
