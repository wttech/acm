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

type ScriptSyncAllDialogProps = {
    onSync: () => void;
};

const ScriptSyncAllDialog: React.FC<ScriptSyncAllDialogProps> = ({ onSync }) => {
    const [syncDialogOpen, setSyncDialogOpen] = useState(false);

    const handleSyncConfirm = async () => {
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
                <p>This action will synchronize all scripts between the author and publish instances. This ensures consistency across the whole environment.</p>
                <p>Notice that <strong>both enabled and disabled</strong> scripts will be synchronized.</p>
            </Content>
            <ButtonGroup>
                <Button variant="secondary" onPress={() => setSyncDialogOpen(false)}>
                    <Cancel />
                    <Text>Cancel</Text>
                </Button>
                <Button variant="negative" style="fill" onPress={handleSyncConfirm}>
                    <Checkmark />
                    <Text>Confirm</Text>
                </Button>
            </ButtonGroup>
        </>
    );

    return (
        <DialogTrigger isOpen={syncDialogOpen} onOpenChange={setSyncDialogOpen}>
            <Button variant="primary" onPress={() => setSyncDialogOpen(true)}>
                <UploadToCloud />
                <Text>Synchronize</Text>
            </Button>
            <Dialog>{renderSyncDialog()}</Dialog>
        </DialogTrigger>
    );
};

export default ScriptSyncAllDialog;