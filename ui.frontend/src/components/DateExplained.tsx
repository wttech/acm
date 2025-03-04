import React from 'react';
import { ContextualHelp, Heading, Content, Text } from '@adobe/react-spectrum';
import { useFormatter } from '../utils/hooks.ts';

interface DateExplainedProps {
    value: string | Date;
}

const DateExplained: React.FC<DateExplainedProps> = ({ value }) => {
    const formatter = useFormatter();

    if (formatter.isTimezoneDifference()) {
        return (
            <>
                <Text>{formatter.dateAtInstance(value)}</Text>
                <ContextualHelp variant="info">
                    <Heading>Timezone Difference</Heading>
                    <Content>
                        <Text>
                            <p>The date and time shown are based on the instance timezone ({formatter.instanceTimezone()}).</p>
                            <p>In your local timezone ({formatter.userTimezone()}), the date and time are {formatter.dateAtUser(value)}.</p>
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