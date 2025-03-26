import React, { useState } from 'react';
import {
    Button,
    Text,
    Dialog,
    Heading,
    Divider,
    Content,
    ButtonGroup,
    DialogContainer,
    Tabs,
    TabList,
    TabPanels,
    Item, View,
} from '@adobe/react-spectrum';
import Gears from '@spectrum-icons/workflow/Gears';
import { apiRequest } from '../utils/api.ts';
import {Argument, ArgumentGroupDefault, ArgumentValue, ArgumentValues, Description} from '../utils/api.types.ts';
import CodeInput from './CodeInput';
import { Strings } from '../utils/strings.ts';
import Close from "@spectrum-icons/workflow/Close";
import Checkmark from "@spectrum-icons/workflow/Checkmark";

interface CodeExecuteButtonProps {
    code: string;
    onExecute: (args: ArgumentValues) => void;
    isDisabled: boolean;
    isPending: boolean;
}

const CodeExecuteButton: React.FC<CodeExecuteButtonProps> = ({ code, onExecute, isDisabled, isPending }) => {
    const [description, setDescription] = useState<Description | null>(null);
    const [args, setArgs] = useState<ArgumentValues>({});
    const [dialogOpen, setDialogOpen] = useState(false);
    const [described, setDescribed] = useState(false);

    const fetchDescription = async () => {
        setDescribed(true);
        try {
            const response = await apiRequest<Description>({
                operation: 'Describe code',
                url: `/apps/acm/api/describe-code.json`,
                method: 'post',
                data: {
                    code: {
                        id: 'console',
                        content: code,
                    },
                },
            });
            const description = response.data.data;
            setDescription(description);

            const initialArgs = description.arguments
                ? Object.fromEntries(Object.entries(description.arguments).map(([key, arg]) => [key, arg.value]))
                : {};
            setArgs(initialArgs);

            const argumentsRequired = description.arguments && Object.keys(description.arguments).length > 0;
            if (argumentsRequired) {
                setDialogOpen(true);
            } else {
                onExecute({});
            }
        } catch (error) {
            console.error('Cannot describe code:', error);
        } finally {
            setDescribed(false);
        }
    };

    const handleExecute = () => {
        if (description) {
            onExecute(args);
        } else {
            fetchDescription();
        }
    };

    const handleCloseDialog = () => {
        setDialogOpen(false);
        setDescription(null);
    };

    const handleArgumentChange = (name: string, value: ArgumentValue) => {
        setArgs({ ...args, [name]: value });
    };

    const descriptionArguments: Argument<ArgumentValue>[] = Object.values(description?.arguments || []);
    const groups = Array.from(new Set(descriptionArguments.map(arg => arg.group)));
    const shouldRenderTabs = groups.length > 1 || (groups.length === 1 && groups[0] !== ArgumentGroupDefault);

    return (
        <>
            <Button variant="accent" onPress={handleExecute} isPending={isPending || described} isDisabled={isDisabled}>
                <Gears />
                <Text>Execute</Text>
            </Button>
            <DialogContainer onDismiss={handleCloseDialog}>
                {dialogOpen && (
                    <Dialog>
                        <Heading>Provide Arguments</Heading>
                        <Divider />
                        <Content>
                            {shouldRenderTabs ? (
                                <Tabs>
                                    <TabList>
                                        {groups.map(group => (
                                            <Item key={group}>{Strings.capitalize(group)}</Item>
                                        ))}
                                    </TabList>
                                    <TabPanels>
                                        {groups.map(group => (
                                            <Item key={group}>
                                                <View marginY="size-200">
                                                    {descriptionArguments
                                                        .filter(arg => arg.group === group)
                                                        .map(arg => (
                                                            <CodeInput
                                                                key={arg.name}
                                                                arg={arg}
                                                                value={args[arg.name]}
                                                                onChange={handleArgumentChange}
                                                            />
                                                        ))}
                                                </View>
                                            </Item>
                                        ))}
                                    </TabPanels>
                                </Tabs>
                            ) : (
                                descriptionArguments.map(arg => (
                                    <CodeInput
                                        key={arg.name}
                                        arg={arg}
                                        value={args[arg.name]}
                                        onChange={handleArgumentChange}
                                    />
                                ))
                            )}
                        </Content>
                        <ButtonGroup>
                            <Button variant="secondary" onPress={handleCloseDialog}>
                                <Close size="XS" />
                                <Text>Cancel</Text>
                            </Button>
                            <Button variant="cta" onPress={() => { handleCloseDialog(); onExecute(args); }}>
                                <Checkmark size="XS" />
                                <Text>Start</Text>
                            </Button>
                        </ButtonGroup>
                    </Dialog>
                )}
            </DialogContainer>
        </>
    );
};

export default CodeExecuteButton;