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

import {ToastQueue} from '@react-spectrum/toast'

const ConsolePage = () => {

    const onExecute = () => {
        fetch('/apps/migrator/api/executor.json', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
        }).then(response => {
            if (response.ok) {
                ToastQueue.positive('Script executed successfully', {timeout: 5000});
                return response.json();
            } else {
                ToastQueue.negative('Script cannot be executed properly!', {timeout: 5000});
            }
        });
    }

    const onAbort = () => {
        ToastQueue.neutral('Abort to be implemented!', {timeout: 5000});
    }

    const onCheckSyntax = () => {
        ToastQueue.neutral('Check syntax to be implemented!', {timeout: 5000});
    }

    const onCopyOutput = () => {
        ToastQueue.neutral('Copy output to be implemented!', {timeout: 5000});
    }

    return (
        <Flex direction="column" gap="size-200">
            <Tabs>
                <TabList>
                    <Item key="code"><FileCode/><Text>Code</Text></Item>
                    <Item key="output"><Print/><Text>Output</Text></Item>
                </TabList>
                <TabPanels>
                    <Item key="code">
                        <Flex direction="column" gap="size-200" marginY="size-100">
                            <ButtonGroup>
                                <Button variant="accent" onPress={onExecute}><Gears/><Text>Execute</Text></Button>
                                <Button variant="secondary" onPress={onCheckSyntax} style="fill"><Spellcheck/><Text>Check syntax</Text></Button>
                            </ButtonGroup>
                            <View backgroundColor="gray-800"
                                  borderWidth="thin"
                                  borderColor="dark"
                                  borderRadius="medium"
                                  padding="size-50">
                                <Editor theme="vs-dark"
                                        defaultValue={ConsoleCode}
                                        height="60vh"
                                        language="java"
                                />
                            </View>
                        </Flex>

                    </Item>
                    <Item key="output">
                        <Flex direction="column" gap="size-200" marginY="size-100">
                            <ButtonGroup>
                                <Button variant="negative" isDisabled={false} onPress={onAbort}><Cancel/><Text>Abort</Text></Button>
                                <Button variant="secondary" onPress={onCopyOutput}><Copy/><Text>Copy</Text></Button>
                            </ButtonGroup>
                            <View backgroundColor="gray-800"
                                  borderWidth="thin"
                                  borderColor="dark"
                                  borderRadius="medium"
                                  padding="size-50">
                                <Editor theme="vs-dark"
                                        value={ConsoleOutput}
                                        height="60vh"
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
