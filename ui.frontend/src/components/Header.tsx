import { Button, Flex, Text } from '@adobe/react-spectrum';
import Code from '@spectrum-icons/workflow/Code';
import Draft from '@spectrum-icons/workflow/Draft';
import FileCode from '@spectrum-icons/workflow/FileCode';
import History from '@spectrum-icons/workflow/History';
import Home from '@spectrum-icons/workflow/Home';
import Maintenance from '@spectrum-icons/workflow/Settings';
import { useLocation } from 'react-router-dom';
import { AppLink } from '../AppLink.tsx';
import { useAppState } from '../hooks/app.ts';
import Toggle from './Toggle.tsx';

const Header = () => {
  const location = useLocation();
  const state = useAppState();

  return (
    <Flex justifyContent="center" gap="size-100" marginBottom="size-200">
      <AppLink to="/">
        <Button variant={location.pathname === '/' ? 'accent' : 'primary'} style="outline">
          <Home />
        </Button>
      </AppLink>
      <Toggle when={state.permissions.console}>
        <AppLink to="/console">
          <Button variant={location.pathname.startsWith('/console') ? 'accent' : 'primary'} style="outline">
            <Draft />
            <Text>Console</Text>
          </Button>
        </AppLink>
      </Toggle>
      <AppLink to="/scripts">
        <Button variant={location.pathname.startsWith('/scripts') ? 'accent' : 'primary'} style="outline">
          <FileCode />
          <Text>Scripts</Text>
        </Button>
      </AppLink>
      <AppLink to="/snippets">
        <Button variant={location.pathname.startsWith('/snippets') ? 'accent' : 'primary'} style="outline">
          <Code />
          <Text>Snippets</Text>
        </Button>
      </AppLink>
      <AppLink to="/history">
        <Button variant={location.pathname.startsWith('/history') ? 'accent' : 'primary'} style="outline">
          <History />
          <Text>History</Text>
        </Button>
      </AppLink>
      <AppLink to="/maintenance">
        <Button variant={location.pathname.startsWith('/maintenance') ? 'accent' : 'primary'} style="outline">
          <Maintenance />
        </Button>
      </AppLink>
    </Flex>
  );
};

export default Header;
