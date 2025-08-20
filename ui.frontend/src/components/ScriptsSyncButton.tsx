import { Button, ButtonGroup, Content, Dialog, DialogTrigger, Divider, Heading, Text } from '@adobe/react-spectrum';
import Refresh from '@spectrum-icons/workflow/Refresh';
import Checkmark from '@spectrum-icons/workflow/Checkmark';
import Cancel from '@spectrum-icons/workflow/Cancel';
import React, { useState } from 'react';
import { toastRequest } from '../utils/api';

type ScriptsSyncButtonProps = {
    selectedKeys: string[];
    onSync: () => void;
};

// TODO support 'all' selection
const ScriptsSyncButton: React.FC<ScriptsSyncButtonProps> = ({ selectedKeys, onSync }) => {
    const [toggleDialogOpen, setToggleDialogOpen] = useState(false);
    const [isLoading, setIsLoading] = useState(false);

    const handleConfirm = async () => {
        setIsLoading(true);
        const ids = Array.from(selectedKeys);

        const params = new URLSearchParams();
        params.append('action', 'sync_all');
        ids.forEach((id) => params.append('id', id));

        try {
            await toastRequest({
                method: 'POST',
                url: `/apps/acm/api/script.json?${params.toString()}`,
                operation: `Synchronize scripts`,
            });
            onSync();
        } catch (error) {
            console.error(`Synchronize scripts error:`, error);
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
                <Text>Synchronizing scripts will update their state across all publish instances.</Text>
            </Content>
            <ButtonGroup>
                <Button variant="secondary" onPress={() => setToggleDialogOpen(false)} isDisabled={isLoading}>
                    <Cancel />
                    <Text>Cancel</Text>
                </Button>
                <Button variant="cta" style="fill" onPress={handleConfirm} isPending={isLoading}>
                    <Checkmark />
                    <Text>Confirm</Text>
                </Button>
            </ButtonGroup>
        </>
    );

    return (
        <DialogTrigger isOpen={toggleDialogOpen} onOpenChange={setToggleDialogOpen}>
            <Button variant='secondary' isDisabled={selectedKeys.length === 0} onPress={() => setToggleDialogOpen(true)}>
                <Refresh />
                <Text>Synchronize</Text>
            </Button>
            <Dialog>{renderToggleDialog()}</Dialog>
        </DialogTrigger>
    );
};

export default ScriptsSyncButton;