import { Content, ContextualHelp, Heading, Link, Text } from '@adobe/react-spectrum';
import React from 'react';
import { Strings } from '../utils/strings.ts';

type UserInfoProps = {
  id: string;
};

const extractFullNameFromEmail = (email: string): string | null => {
  const match = /^([a-zA-Z]+)\.([a-zA-Z]+)@/.exec(email);
  if (match) {
    const [, first, last] = match;
    return `${Strings.capitalize(first)} ${Strings.capitalize(last)}`;
  }
  return null;
};

const extractUserFromEmail = (email: string): string | null => {
  if (email.includes('@')) {
    const parts = email.split('@');
    return parts.length > 0 ? parts[0] : null;
  }
  return null;
}

const UserInfo: React.FC<UserInfoProps> = ({ id }) => {
  // For example, john.doe@acme.com => John Doe
  const name = extractFullNameFromEmail(id);
  if (name) {
    return (
      <>
        <Text>{name}</Text>
        <ContextualHelp variant="info">
          <Heading>Email</Heading>
          <Content>
            <Link href={`mailto:${id}`}>{id}</Link>
          </Content>
        </ContextualHelp>
      </>
    );
  }

  // For example, jdoe@acme.com => jdoe
  const user = extractUserFromEmail(id)
  if (user) {
    return (
      <>
        <Text>{user}</Text>
        <ContextualHelp variant="info">
          <Heading>Email</Heading>
          <Content>
            <Link href={`mailto:${id}`}>{id}</Link>
          </Content>
        </ContextualHelp>
      </>
    );
  }

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

  // All other cases
  return <Text>{id}</Text>;
};

export default UserInfo;
