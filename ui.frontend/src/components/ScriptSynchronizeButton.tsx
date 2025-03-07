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
import UploadToCloud from "@spectrum-icons/workflow/UploadToCloud";

type ScriptSynchronizeButtonProps = {
    selectedKeys: string[];
    onSync: () => void;
};

const ScriptSynchronizeButton: React.FC<ScriptSynchronizeButtonProps> = ({ selectedKeys, onSync }) => {
    const [syncDialogOpen, setSyncDialogOpen] = useState(false);
    const [isLoading, setIsLoading] = useState(false);

    const handleSyncConfirm = async () => {
        setIsLoading(true);
        try {
            await toastRequest({
                method: 'POST',
                url: `/apps/contentor/api/script.json?action=sync_all`,
                operation: `Synchronize scripts`,
            });
            onSync();
        } catch (error) {
            console.error(`Synchronize scripts error:`, error);
        } finally {
            setIsLoading(false);
            setSyncDialogOpen(false);
        }
    };

    const renderSyncDialog = () => (
        <>
            <Heading>
                <Text>Confirmation</Text>
            </Heading>
            <Divider />
            <Content>
                <p>This action will synchronize <strong>all scripts</strong> between the author and publish instances. This ensures consistency across the whole environment.</p>
                <p>Notice that <strong>both enabled and disabled</strong> scripts will be synchronized.</p>
            </Content>
            <ButtonGroup>
                <Button variant="secondary" onPress={() => setSyncDialogOpen(false)} isDisabled={isLoading}>
                    <Cancel />
                    <Text>Cancel</Text>
                </Button>
                <Button variant="negative" style="fill" onPress={handleSyncConfirm} isPending={isLoading}>
                    <Checkmark />
                    <Text>Confirm</Text>
                </Button>
            </ButtonGroup>
        </>
    );

    return (
        <DialogTrigger isOpen={syncDialogOpen} onOpenChange={setSyncDialogOpen}>
            <Button variant="primary" onPress={() => setSyncDialogOpen(true)} isDisabled={selectedKeys.length > 0}>
                <UploadToCloud />
                <Text>Synchronize</Text>
            </Button>
            <Dialog>{renderSyncDialog()}</Dialog>
        </DialogTrigger>
    );
};

export default ScriptSynchronizeButton;