import React, { useState } from 'react';
import {
    Button,
    ButtonGroup,
    Content,
    Dialog,
    DialogTrigger,
    Divider,
    Heading,
    Text
} from '@adobe/react-spectrum';
import { toastRequest } from '../utils/api';
import Cancel from '@spectrum-icons/workflow/Cancel';
import Checkmark from '@spectrum-icons/workflow/Checkmark';
import {QueueOutput} from "../utils/api.types.ts";

type ExecutionAbortButtonProps = {
    selectedKeys: string[];
    onAbort?: () => void;
};

const ExecutionAbortButton: React.FC<ExecutionAbortButtonProps> = ({ selectedKeys, onAbort }) => {
    const [abortDialogOpen, setAbortDialogOpen] = useState(false);
    const [isLoading, setIsLoading] = useState(false);

    const handleConfirm = async () => {
        setIsLoading(true);
        const ids = Array.from(selectedKeys);

        const params = new URLSearchParams();
        ids.forEach((id) => params.append('jobId', id));

        try {
            await toastRequest<QueueOutput>({
                method: 'DELETE',
                url: `/apps/contentor/api/queue-code.json?${params.toString()}`,
                operation: 'Abort executions',
            });
            if (onAbort) onAbort();
        } catch (error) {
            console.error('Abort executions error:', error);
        } finally {
            setIsLoading(false);
            setAbortDialogOpen(false);
        }
    };

    const renderAbortDialog = () => (
        <>
            <Heading>
                <Text>Confirmation</Text>
            </Heading>
            <Divider />
            <Content>
                <Text>Are you sure you want to abort the selected executions? This action cannot be undone.</Text>
            </Content>
            <ButtonGroup>
                <Button variant="secondary" onPress={() => setAbortDialogOpen(false)} isDisabled={isLoading}>
                    <Cancel />
                    <Text>Cancel</Text>
                </Button>
                <Button variant="negative" style="fill" onPress={handleConfirm} isPending={isLoading}>
                    <Checkmark />
                    <Text>Confirm</Text>
                </Button>
            </ButtonGroup>
        </>
    );

    return (
        <DialogTrigger isOpen={abortDialogOpen} onOpenChange={setAbortDialogOpen}>
            <Button variant="negative" style="fill" isDisabled={selectedKeys.length === 0} onPress={() => setAbortDialogOpen(true)}>
                <Cancel />
                <Text>Abort</Text>
            </Button>
            <Dialog>{renderAbortDialog()}</Dialog>
        </DialogTrigger>
    );
};

export default ExecutionAbortButton;