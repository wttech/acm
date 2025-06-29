import { Content, ContextualHelp, Heading, Text } from '@adobe/react-spectrum';
import React from 'react';
import { useFormatter } from '../hooks/formatter';

interface DateExplainedProps {
  value: string | Date;
}

const DateExplained: React.FC<DateExplainedProps> = ({ value }) => {
  const formatter = useFormatter();

  if (!value) {
    return <Text>&mdash;</Text>;
  }

  if (formatter.isTimezoneDifference()) {
    return (
      <>
        <Text>{formatter.dateAtInstance(value)}</Text>
        &nbsp;
        <ContextualHelp variant="help">
          <Heading>Timezone Difference</Heading>
          <Content>
            <Text>
              <p>The date and time shown are based on the instance timezone ({formatter.instanceTimezone()}).</p>
              <p>
                In your local timezone ({formatter.userTimezone()}), the date and time are {formatter.dateAtUser(value)}.
              </p>
              <p>This was {formatter.dateRelative(value)}.</p>
            </Text>
          </Content>
        </ContextualHelp>
      </>
    );
  }

  return <Text>{formatter.dateAtInstance(value)}</Text>;
};

export default DateExplained;
