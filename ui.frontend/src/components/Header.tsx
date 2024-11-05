import {AppLink} from "../AppLink.tsx";
import {Button, Flex, Text} from "@adobe/react-spectrum";
import Draft from "@spectrum-icons/workflow/Draft";
import FileCode from "@spectrum-icons/workflow/FileCode";
import {useLocation} from "react-router-dom";

const Header = () => {
    const location = useLocation();

    return (
        <Flex wrap gap="size-200">
            <AppLink to="/console">
                <Button variant={location.pathname === '/console' ? 'accent' : 'primary'} style="outline">
                    <Draft/>
                    <Text>Console</Text>
                </Button>
            </AppLink>
            <AppLink to="/scripts">
                <Button variant={location.pathname === '/scripts' ? 'accent' : 'primary'} style="outline">
                    <FileCode/>
                    <Text>Scripts</Text>
                </Button>
            </AppLink>
        </Flex>
    );
};

export default Header;
