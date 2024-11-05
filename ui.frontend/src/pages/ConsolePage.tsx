import {Button, ButtonGroup, Flex, View, Text} from "@adobe/react-spectrum";
import Editor from "@monaco-editor/react";
import ConsoleScript from "./ConsoleScript.groovy";
import Spellcheck from "@spectrum-icons/workflow/Spellcheck";
import Gears from "@spectrum-icons/workflow/Gears";

const ConsolePage = () => {

    return (
        <Flex direction="column" gap="size-200">
            <ButtonGroup>
                <Button variant="accent"><Gears/><Text>Execute</Text></Button>
                <Button variant="secondary" style="fill"><Spellcheck/><Text>Check syntax</Text></Button>
            </ButtonGroup>
            <View borderWidth="thin"
                  borderColor="dark"
                  borderRadius="medium"
                  padding="size-100">
                <Editor theme="vs-dark"
                        defaultValue={ConsoleScript}
                        height="70vh"
                        language="java"
                />
            </View>
        </Flex>
    );
};

export default ConsolePage;
