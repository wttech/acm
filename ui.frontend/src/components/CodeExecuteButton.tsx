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
} from '@adobe/react-spectrum';
import Gears from '@spectrum-icons/workflow/Gears';
import { apiRequest } from '../utils/api.ts';
import {Argument, ArgumentValue, ArgumentValues, Description} from '../utils/api.types.ts';
import CodeInput from './CodeInput';

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

    const fetchDescription = async () => {
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

            const argumentsRequired = description.arguments && Object.keys(description.arguments).length > 0;
            if (argumentsRequired) {
                setDialogOpen(true);
            } else {
                onExecute({});
            }
        } catch (error) {
            console.error('Cannot describe code:', error);
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

    const descriptionArguments: Argument[] = Object.values(description?.arguments || []);

    return (
        <>
            <Button variant="accent" onPress={handleExecute} isPending={isPending} isDisabled={isDisabled}>
                <Gears />
                <Text>Execute</Text>
            </Button>
            <DialogContainer onDismiss={handleCloseDialog}>
                {dialogOpen && (
                    <Dialog>
                        <Heading>Provide Arguments</Heading>
                        <Divider />
                        <Content>
                            {descriptionArguments.map((arg: Argument) => (
                                <CodeInput
                                    key={arg.name}
                                    arg={arg}
                                    value={args[arg.name]}
                                    onChange={handleArgumentChange}
                                />
                            ))}
                        </Content>
                        <ButtonGroup>
                            <Button variant="secondary" onPress={handleCloseDialog}>
                                Cancel
                            </Button>
                            <Button variant="cta" onPress={() => { handleCloseDialog(); onExecute(args); }}>
                                Execute
                            </Button>
                        </ButtonGroup>
                    </Dialog>
                )}
            </DialogContainer>
        </>
    );
};

export default CodeExecuteButton;