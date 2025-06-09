import { Content, ContextualHelp, Heading, Link, Text } from '@adobe/react-spectrum';
import React from 'react';
import { UserIdServicePrefix } from '../utils/api.types';
import { Strings } from '../utils/strings';

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
};

const UserInfo: React.FC<UserInfoProps> = ({ id }) => {
  // ACM service IDs like 'acm-content-service'
  if (id.startsWith(UserIdServicePrefix)) {
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

  // For example, john.doe@acme.com => John Doe, jdoe@acme.com => jdoe
  const idShort = extractFullNameFromEmail(id) || extractUserFromEmail(id);
  if (idShort) {
    return (
      <>
        <Text>{idShort}</Text>
        <ContextualHelp variant="info">
          <Heading>E-mail</Heading>
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
