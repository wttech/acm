import { Button, ButtonGroup, Content, Dialog, DialogTrigger, Divider, Heading, Text } from '@adobe/react-spectrum';
import Cancel from '@spectrum-icons/workflow/Cancel';
import Checkmark from '@spectrum-icons/workflow/Checkmark';
import React, { useState } from 'react';
import { toastRequest } from '../utils/api';

type ScriptsDeleteButtonProps = {
    selectedKeys: string[];
    onDelete: () => void;
};

// TODO support 'all' selection
const ScriptsDeleteButton: React.FC<ScriptsDeleteButtonProps> = ({ selectedKeys, onDelete }) => {
    const [toggleDialogOpen, setToggleDialogOpen] = useState(false);
    const [isLoading, setIsLoading] = useState(false);

    const handleConfirm = async () => {
        setIsLoading(true);
        const ids = Array.from(selectedKeys);

        const params = new URLSearchParams();
        params.append('action', 'delete');
        ids.forEach((id) => params.append('id', id));

        try {
            await toastRequest({
                method: 'POST',
                url: `/apps/acm/api/script.json?${params.toString()}`,
                operation: `Delete scripts`,
            });
            onDelete();
        } catch (error) {
            console.error(`Delete scripts error:`, error);
        } finally {
            setIsLoading(false);
            setToggleDialogOpen(false);
        }
    };

    const renderToggleDialog = () => (
        <>
            <Heading>
                <Text>Confirmation</Text>
            </Heading>
            <Divider />
            <Content>
                <Text>Deleting scripts will stop their automatic execution.</Text>
            </Content>
            <ButtonGroup>
                <Button variant="secondary" onPress={() => setToggleDialogOpen(false)} isDisabled={isLoading}>
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
        <DialogTrigger isOpen={toggleDialogOpen} onOpenChange={setToggleDialogOpen}>
            <Button variant='negative' style="fill" isDisabled={selectedKeys.length === 0} onPress={() => setToggleDialogOpen(true)}>
                <Cancel />
                <Text>Delete</Text>
            </Button>
            <Dialog>{renderToggleDialog()}</Dialog>
        </DialogTrigger>
    );
};

export default ScriptsDeleteButton;
