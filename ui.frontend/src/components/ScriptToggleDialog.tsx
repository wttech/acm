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

type ScriptToggleDialogProps = {
    type: 'enabled' | 'disabled';
    selectedKeys: string[];
    onToggle: () => void;
};

const ScriptToggleDialog: React.FC<ScriptToggleDialogProps> = ({ type, selectedKeys, onToggle }) => {
    const [toggleDialogOpen, setToggleDialogOpen] = useState(false);

    const handleConfirm = async () => {
        const action = type === 'enabled' ? 'disable' : 'enable';
        const ids = Array.from(selectedKeys);

        const params = new URLSearchParams();
        params.append('action', action);
        params.append('type', type);
        ids.forEach((id) => params.append('id', id));

        try {
            await toastRequest({
                method: 'POST',
                url: `/apps/contentor/api/script.json?${params.toString()}`,
                operation: `${action} scripts`,
            });
            onToggle();
        } catch (error) {
            console.error(`${action} scripts error:`, error);
        } finally {
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
                {type === 'enabled' ? (
                    <Text>Disabling scripts will stop their automatic execution. To execute them again, you need to enable them or reinstall the package with scripts.</Text>
                ) : (
                    <Text>Enabling scripts can cause changes in the repository and potential data loss. Ensure the script is ready to use. It is recommended to provide enabled scripts via a package, not manually.</Text>
                )}
            </Content>
            <ButtonGroup>
                <Button variant="secondary" onPress={() => setToggleDialogOpen(false)}>
                    <Cancel />
                    <Text>Cancel</Text>
                </Button>
                <Button variant="negative" style="fill" onPress={handleConfirm}>
                    <Checkmark />
                    <Text>Confirm</Text>
                </Button>
            </ButtonGroup>
        </>
    );

    return (
        <DialogTrigger isOpen={toggleDialogOpen} onOpenChange={setToggleDialogOpen}>
            <Button variant={type === 'enabled' ? 'negative' : 'accent'} style="fill" isDisabled={selectedKeys.length === 0} onPress={() => setToggleDialogOpen(true)}>
                {type === 'enabled' ? <Cancel /> : <Checkmark />}
                <Text>{type === 'enabled' ? 'Disable' : 'Enable'}</Text>
            </Button>
            <Dialog>{renderToggleDialog()}</Dialog>
        </DialogTrigger>
    );
};

export default ScriptToggleDialog;