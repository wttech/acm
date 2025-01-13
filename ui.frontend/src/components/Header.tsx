import { Button, Flex, Text } from '@adobe/react-spectrum';
import Code from '@spectrum-icons/workflow/Code';
import Draft from '@spectrum-icons/workflow/Draft';
import FileCode from '@spectrum-icons/workflow/FileCode';
import History from '@spectrum-icons/workflow/History';
import { useLocation } from 'react-router-dom';
import { AppLink } from '../AppLink.tsx';

const Header = () => {
  const location = useLocation();

  return (
    <Flex justifyContent="center" gap="size-100" marginBottom="size-200">
      <AppLink to="/console">
        <Button variant={location.pathname === '/console' ? 'accent' : 'primary'} style="outline">
          <Draft />
          <Text>Console</Text>
        </Button>
      </AppLink>
      <AppLink to="/snippets">
        <Button variant={location.pathname === '/snippets' ? 'accent' : 'primary'} style="outline">
          <Code />
          <Text>Snippets</Text>
        </Button>
      </AppLink>
      <AppLink to="/scripts">
        <Button variant={location.pathname === '/scripts' ? 'accent' : 'primary'} style="outline">
          <FileCode />
          <Text>Scripts</Text>
        </Button>
      </AppLink>
      <AppLink to="/executions">
        <Button variant={location.pathname === '/executions' ? 'accent' : 'primary'} style="outline">
          <History />
          <Text>Executions</Text>
        </Button>
      </AppLink>
    </Flex>
  );
};

export default Header;
