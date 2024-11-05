import {Button, ButtonGroup, Flex, Heading, View, Text} from "@adobe/react-spectrum";
import Editor from "@monaco-editor/react";
import ConsoleScript from "./ConsoleScript.groovy";
import Spellcheck from "@spectrum-icons/workflow/Spellcheck";
import Gears from "@spectrum-icons/workflow/Gears";

const ConsolePage = () => {

    return (
        <Flex direction="column" gap="size-200">
            <View>
                <Heading>Console</Heading>
                <p>Write your Groovy script here.</p>
            </View>
            <View borderWidth="thin"
                  borderColor="dark"
                  borderRadius="medium"
                  padding="size-100">
                <Editor theme="vs-dark"
                        defaultValue={ConsoleScript}
                        height="480px"
                        language="java"
                />
            </View>
            <ButtonGroup>
                <Button variant="secondary"><Spellcheck/><Text>Check syntax</Text></Button>
                <Button variant="accent"><Gears/><Text>Execute</Text></Button>
            </ButtonGroup>
        </Flex>
    );
};

export default ConsolePage;
