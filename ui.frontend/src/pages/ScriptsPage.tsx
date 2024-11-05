import {Cell, Column, Flex, Heading, Row, TableBody, TableHeader, TableView, View} from "@adobe/react-spectrum";

const ScriptsPage = () => {
    return (
        <Flex direction="column">
            <View>
                <Heading>Scripts</Heading>
                <p>Manage your migration scripts here.</p>
            </View>
            <TableView
                aria-label="Example table with static contents"
                selectionMode="multiple"
            >
                <TableHeader>
                    <Column>Name</Column>
                    <Column>Type</Column>
                    <Column align="end">Date Modified</Column>
                </TableHeader>
                <TableBody>
                    <Row>
                        <Cell>Games</Cell>
                        <Cell>File folder</Cell>
                        <Cell>6/7/2020</Cell>
                    </Row>
                    <Row>
                        <Cell>Program Files</Cell>
                        <Cell>File folder</Cell>
                        <Cell>4/7/2021</Cell>
                    </Row>
                    <Row>
                        <Cell>bootmgr</Cell>
                        <Cell>System file</Cell>
                        <Cell>11/20/2010</Cell>
                    </Row>
                    <Row>
                        <Cell>log.txt</Cell>
                        <Cell>Text Document</Cell>
                        <Cell>1/18/2016</Cell>
                    </Row>
                </TableBody>
            </TableView>
        </Flex>
    );
};

export default ScriptsPage;
