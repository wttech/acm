import {ActionGroup, Button, Flex, Item, Text} from '@adobe/react-spectrum';
import Code from '@spectrum-icons/workflow/Code';
import Draft from '@spectrum-icons/workflow/Draft';
import FileCode from '@spectrum-icons/workflow/FileCode';
import History from '@spectrum-icons/workflow/History';
import { useLocation } from 'react-router-dom';
import { AppLink } from '../AppLink.tsx';
import Maintenance from "@spectrum-icons/workflow/Settings";
import Home from "@spectrum-icons/workflow/Home";

const Header = () => {
  const location = useLocation();

  return (
    <Flex justifyContent="center" gap="size-100" marginBottom="size-200">
      <AppLink to="/">
        <Button variant={location.pathname === '/' ? 'accent' : 'primary'} style="outline">
            <Home />
        </Button>
      </AppLink>
      <AppLink to="/console">
        <Button variant={location.pathname.startsWith('/console') ? 'accent' : 'primary'} style="outline">
          <Draft />
          <Text>Console</Text>
        </Button>
      </AppLink>
      <AppLink to="/snippets">
        <Button variant={location.pathname.startsWith('/snippets') ? 'accent' : 'primary'} style="outline">
          <Code />
          <Text>Snippets</Text>
        </Button>
      </AppLink>
      <AppLink to="/scripts">
        <Button variant={location.pathname.startsWith('/scripts') ? 'accent' : 'primary'} style="outline">
          <FileCode />
          <Text>Scripts</Text>
        </Button>
      </AppLink>
      <AppLink to="/executions">
        <Button variant={location.pathname.startsWith('/executions') ? 'accent' : 'primary'} style="outline">
          <History />
          <Text>Executions</Text>
        </Button>
      </AppLink>
      <AppLink to="/maintenance">
        <Button variant={location.pathname.startsWith('/maintenance') ? 'accent' : 'primary'} style="outline">
            <Maintenance/>
        </Button>
      </AppLink>
    </Flex>
  );
};

export default Header;
