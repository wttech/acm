import { Content, ContextualHelp, Heading, Link, Text } from '@adobe/react-spectrum';
import React from 'react';
import { Strings } from '../utils/strings.ts';

type UserInfoProps = {
  id: string;
};

const getNameFromEmail = (email: string): string | null => {
  const match = /^([a-zA-Z]+)\.([a-zA-Z]+)@/.exec(email);
  if (match) {
    const [, first, last] = match;
    return `${Strings.capitalize(first)} ${Strings.capitalize(last)}`;
  }
  return null;
};

const UserInfo: React.FC<UserInfoProps> = ({ id }) => {
  // ACM service IDs
  if (id.startsWith('acm-')) {
    return (
      <>
        <Text>acm</Text>
        <ContextualHelp variant="info">
          <Heading>User ID</Heading>
          <Content>{id}</Content>
        </ContextualHelp>
      </>
    );
  }

  // Email with the single dot
  const name = getNameFromEmail(id);
  if (name) {
    return (
      <>
        <Text>{name}</Text>
        <ContextualHelp variant="info">
          <Heading>User ID</Heading>
          <Content>
            <Link href={`mailto:${id}`}>{id}</Link>
          </Content>
        </ContextualHelp>
      </>
    );
  }

  // All other cases
  return <Text>{id}</Text>;
};

export default UserInfo;
