import React, { useState } from 'react';
import { Button, Text, Dialog, Heading, Divider, Content, ButtonGroup } from '@adobe/react-spectrum';
import Gears from '@spectrum-icons/workflow/Gears';
import { apiRequest } from '../utils/api.ts';
import { Argument, Description } from '../utils/api.types.ts';

type ArgumentValue = string | number | boolean | null | undefined;

interface CodeExecuteButtonProps {
    code: string;
    onExecute: (args: Record<string, ArgumentValue>) => void;
    isPending: boolean;
}

const CodeExecuteButton: React.FC<CodeExecuteButtonProps> = ({ code, onExecute, isPending }) => {
    const [description, setDescription] = useState<Description | null>(null);
    const [args, setArgs] = useState<Record<string, ArgumentValue>>({});
    const [showDialog, setShowDialog] = useState(false);

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
            if (description.arguments && Object.keys(description.arguments).length > 0) {
                setShowDialog(true);
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

    return (
        <>
            <Button variant="accent" onPress={handleExecute} isPending={isPending} isDisabled={isPending}>
                <Gears />
                <Text>Execute</Text>
            </Button>
            {showDialog && (
                <Dialog>
                    <Heading>Provide Arguments</Heading>
                    <Divider />
                    <Content>
                        {(Object.values(description?.arguments || []) || []).map((arg: Argument) => (
                            <div key={arg.name}>
                                <label>{arg.name}</label>
                                <input type="text" value={args[arg.name]?.toString() || ''} onChange={(e) => setArgs({ ...args, [arg.name]: e.target.value })} />
                            </div>
                        ))}
                    </Content>
                    <ButtonGroup>
                        <Button variant="secondary" onPress={() => setShowDialog(false)}>
                            Cancel
                        </Button>
                        <Button variant="cta" onPress={() => { setShowDialog(false); onExecute(args); }}>
                            Execute
                        </Button>
                    </ButtonGroup>
                </Dialog>
            )}
        </>
    );
};

export default CodeExecuteButton;