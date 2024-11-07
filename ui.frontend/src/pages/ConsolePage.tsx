import {Button, ButtonGroup, Flex, View, Text, Tabs, TabList, Item, TabPanels} from "@adobe/react-spectrum";
import Editor from "@monaco-editor/react";
import ConsoleCode from "./ConsoleCode.groovy";
import Spellcheck from "@spectrum-icons/workflow/Spellcheck";
import Gears from "@spectrum-icons/workflow/Gears";
import FileCode from "@spectrum-icons/workflow/FileCode";
import Print from "@spectrum-icons/workflow/Print";
import Copy from "@spectrum-icons/workflow/Copy";
import Cancel from "@spectrum-icons/workflow/Cancel";

import {ToastQueue} from '@react-spectrum/toast'
import {toastRequest} from "../utils/api.ts";
import {useState} from "react";

const ConsolePage = () => {
    const [code, setCode] = useState(ConsoleCode);
    const [output, setOutput] = useState('');

    const onExecute = async () => {
        await toastRequest({
            operation: 'Script execution',
            url: `/apps/migrator/api/execute-code.json`,
            method: 'post',
            data: {
                code: {
                    id: 'console',
                    content: code,
                }
            },
            onSuccess: (response) => {
                setOutput(response.data.data.output);
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
                                        value={code}
                                        onChange={setCode}
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
                                        value={output}
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
