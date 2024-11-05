import {Cell, Column, Flex, Row, TableBody, TableHeader, TableView} from "@adobe/react-spectrum";
// import Draw from "@spectrum-icons/workflow/Draw";
// import Copy from "@spectrum-icons/workflow/Copy";
// import Delete from "@spectrum-icons/workflow/Delete";

const ScriptsPage = () => {
    return (
        <Flex direction="column">
            <TableView
                aria-label="Example table with static contents"
                selectionMode="multiple"
            >
                <TableHeader>
                    <Column>Name</Column>
                    <Column>Modified At</Column>
                </TableHeader>
                <TableBody>
                    <Row>
                        <Cell>migrate.text-component.groovy</Cell>
                        <Cell>2024-11-05 13:32:23</Cell>
                    </Row>
                    <Row>
                        <Cell>permissions.everyone.groovy</Cell>
                        <Cell>2024-10-03 20:12:55</Cell>
                    </Row>
                </TableBody>
            </TableView>
        </Flex>
    );
};

export default ScriptsPage;
