import {Button, ButtonGroup, Flex, View, Text, Tabs, TabList, Item, TabPanels} from "@adobe/react-spectrum";
import Editor from "@monaco-editor/react";
import ConsoleCode from "./ConsoleCode.groovy";
import ConsoleOutput from "./ConsoleOutput.txt";
import Spellcheck from "@spectrum-icons/workflow/Spellcheck";
import Gears from "@spectrum-icons/workflow/Gears";
import FileCode from "@spectrum-icons/workflow/FileCode";
import Print from "@spectrum-icons/workflow/Print";
import Copy from "@spectrum-icons/workflow/Copy";
import Cancel from "@spectrum-icons/workflow/Cancel";

const ConsolePage = () => {

    return (
        <Flex direction="column" gap="size-200">

            <Tabs aria-label="History of Ancient Rome">
                <TabList>
                    <Item key="code"><FileCode/><Text>Code</Text></Item>
                    <Item key="output"><Print/><Text>Output</Text></Item>
                </TabList>
                <TabPanels>
                    <Item key="code">
                        <Flex direction="column" gap="size-200" marginY="size-100">
                            <ButtonGroup>
                                <Button variant="accent"><Gears/><Text>Execute</Text></Button>
                                <Button variant="secondary" style="fill"><Spellcheck/><Text>Check syntax</Text></Button>
                            </ButtonGroup>
                            <View backgroundColor="gray-800"
                                  borderWidth="thin"
                                  borderColor="dark"
                                  borderRadius="medium"
                                  padding="size-50">
                                <Editor theme="vs-dark"
                                        defaultValue={ConsoleCode}
                                        height="70vh"
                                        language="java"
                                />
                            </View>
                        </Flex>

                    </Item>
                    <Item key="output">
                        <Flex direction="column" gap="size-200" marginY="size-100">
                            <ButtonGroup>
                                <Button variant="negative" isDisabled={false}><Cancel/><Text>Abort</Text></Button>
                                <Button variant="secondary"><Copy/><Text>Copy</Text></Button>
                            </ButtonGroup>
                            <View backgroundColor="gray-800"
                                  borderWidth="thin"
                                  borderColor="dark"
                                  borderRadius="medium"
                                  padding="size-50">
                                <Editor theme="vs-dark"
                                        value={ConsoleOutput}
                                        height="70vh"
                                        language="java"
                                        options={{readOnly: true}}
                                />
                            </View>
                        </Flex>
                    </Item>
                </TabPanels>
            </Tabs>
        </Flex>
    );
};

export default ConsolePage;
