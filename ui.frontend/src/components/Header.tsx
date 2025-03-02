import {ActionGroup, Button, Flex, Item, Text} from '@adobe/react-spectrum';
import Code from '@spectrum-icons/workflow/Code';
import Draft from '@spectrum-icons/workflow/Draft';
import FileCode from '@spectrum-icons/workflow/FileCode';
import History from '@spectrum-icons/workflow/History';
import { useLocation } from 'react-router-dom';
import { AppLink } from '../AppLink.tsx';
import Draw from "@spectrum-icons/workflow/Draw";
import Copy from "@spectrum-icons/workflow/Copy";
import Delete from "@spectrum-icons/workflow/Delete";
import Move from "@spectrum-icons/workflow/Move";
import Duplicate from "@spectrum-icons/workflow/Duplicate";
import Settings from "@spectrum-icons/workflow/Settings";

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
      <AppLink to="/settings">
        <Button variant={location.pathname === '/settings' ? 'accent' : 'primary'} style="outline">
            <Settings/>
        </Button>
      </AppLink>
    </Flex>
  );
};

export default Header;
