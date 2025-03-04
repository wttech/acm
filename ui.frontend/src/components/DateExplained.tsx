import React from 'react';
import { ContextualHelp, Heading, Content, Text } from '@adobe/react-spectrum';
import { useFormatter } from '../utils/hooks.ts';
import styles from './DateExplained.module.css';

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
                        <table className={styles.table}>
                            <thead>
                            <tr>
                                <th>Location</th>
                                <th>Timezone</th>
                                <th>Date</th>
                            </tr>
                            </thead>
                            <tbody>
                            <tr>
                                <td>Client</td>
                                <td>{formatter.userTimezone()}</td>
                                <td>{formatter.dateAtUser(value)}</td>
                            </tr>
                            <tr>
                                <td>Instance</td>
                                <td>{formatter.instanceTimezone()}</td>
                                <td>{formatter.dateAtInstance(value)}</td>
                            </tr>
                            </tbody>
                        </table>
                    </Content>
                </ContextualHelp>
            </>
        );
    }

    return <Text>{formatter.dateAtInstance(value)}</Text>;
};

export default DateExplained;