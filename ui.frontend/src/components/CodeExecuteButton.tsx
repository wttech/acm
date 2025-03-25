import React, { useState } from 'react';
import { Button, Text, Dialog, Heading, Divider, Content, ButtonGroup, DialogContainer, TextField, Checkbox, NumberField, Picker, Item } from '@adobe/react-spectrum';
import Gears from '@spectrum-icons/workflow/Gears';
import { apiRequest } from '../utils/api.ts';
import { Argument, Description, isToggleArgument, isStringArgument, isTextArgument, isSelectArgument, isNumberArgument } from '../utils/api.types.ts';

type ArgumentValue = string | number | boolean | null | undefined;

interface CodeExecuteButtonProps {
    code: string;
    onExecute: (args: Record<string, ArgumentValue>) => void;
    isPending: boolean;
}

const CodeExecuteButton: React.FC<CodeExecuteButtonProps> = ({ code, onExecute, isPending }) => {
    const [description, setDescription] = useState<Description | null>(null);
    const [args, setArgs] = useState<Record<string, ArgumentValue>>({});
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

    const renderArgumentField = (arg: Argument) => {
        if (isToggleArgument(arg)) {
            return (
                <div key={arg.name} style={{ marginBottom: '1rem' }}>
                    <Checkbox
                        isSelected={args[arg.name] as boolean}
                        onChange={(value) => setArgs({ ...args, [arg.name]: value })}
                    >
                        {arg.label || arg.name}
                    </Checkbox>
                </div>
            );
        } else if (isStringArgument(arg)) {
            return (
                <div key={arg.name} style={{ marginBottom: '1rem' }}>
                    <TextField
                        label={arg.label || arg.name}
                        value={args[arg.name]?.toString() || ''}
                        onChange={(value) => setArgs({ ...args, [arg.name]: value })}
                    />
                </div>
            );
        } else if (isTextArgument(arg)) {
            return (
                <div key={arg.name} style={{ marginBottom: '1rem' }}>
                    <TextField
                        label={arg.label || arg.name}
                        value={args[arg.name]?.toString() || ''}
                        onChange={(value) => setArgs({ ...args, [arg.name]: value })}
                        multiline
                    />
                </div>
            );
        } else if (isSelectArgument(arg)) {
            return (
                <div key={arg.name} style={{ marginBottom: '1rem' }}>
                    <Picker
                        label={arg.label || arg.name}
                        selectedKey={args[arg.name]?.toString() || ''}
                        onSelectionChange={(value) => setArgs({ ...args, [arg.name]: value })}
                    >
                        {Object.entries(arg.options).map(([key, value]) => (
                            <Item key={key}>{value}</Item>
                        ))}
                    </Picker>
                </div>
            );
        } else if (isNumberArgument(arg)) {
            return (
                <div key={arg.name} style={{ marginBottom: '1rem' }}>
                    <NumberField
                        label={arg.label || arg.name}
                        value={args[arg.name] as number}
                        onChange={(value) => setArgs({ ...args, [arg.name]: value })}
                        minValue={arg.min}
                        maxValue={arg.max}
                    />
                </div>
            );
        } else {
            return null;
        }
    };

    const descriptionArguments = Object.values(description?.arguments || []);

    return (
        <>
            <Button variant="accent" onPress={handleExecute} isPending={isPending} isDisabled={isPending}>
                <Gears />
                <Text>Execute</Text>
            </Button>
            <DialogContainer onDismiss={handleCloseDialog}>
                {dialogOpen && (
                    <Dialog>
                        <Heading>Provide Arguments</Heading>
                        <Divider />
                        <Content>
                            {descriptionArguments.map((arg: Argument) => renderArgumentField(arg))}
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