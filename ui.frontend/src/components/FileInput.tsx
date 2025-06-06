import { FileTrigger, ProgressCircle, Button, ListView, Item, Text, Flex } from '@adobe/react-spectrum';
import { useState } from 'react';
import { isMultiFileArgument, FileOutput, FileArgument } from '../utils/api.types.ts';
import { apiRequest } from '../utils/api';
import Delete from '@spectrum-icons/workflow/Delete';

interface FileFieldProps {
    arg: FileArgument; // TODO make it more reusable
    value: string | string[];
    onChange: (name: string, value: string | string[]) => void;
}

type FileItem = {
    name: string;
    path: string;
    uploading: boolean;
    deleting: boolean;
    file?: File;
};

const uploadFiles = async (files: File[]): Promise<string[]> => {
    const formData = new FormData();
    files.forEach((file) => formData.append('file', file));
    const response = await apiRequest<FileOutput>({
        operation: 'File upload',
        url: '/apps/acm/api/file.json',
        method: 'POST',
        data: formData,
        headers: {}, // Let browser set Content-Type for FormData
    });
    return response.data.data.files;
};

const deleteFile = async (path: string): Promise<void> => {
    await apiRequest<FileOutput>({
        operation: 'File delete',
        url: '/apps/acm/api/file.json',
        method: 'DELETE',
        data: { path },
        headers: { 'Content-Type': 'application/json' },
    });
};

const FileField: React.FC<FileFieldProps> = ({ arg, value, onChange }) => {
    const [files, setFiles] = useState<FileItem[]>(
        (Array.isArray(value) ? value : value ? [value] : []).map((path) => ({
            name: path.split('/').pop() ?? path,
            path,
            uploading: false,
            deleting: false,
        }))
    );

    const handleFiles = async (selectedFiles: FileList | null) => {
        if (!selectedFiles) {
            return;
        }
        const newFiles: FileItem[] = Array.from(selectedFiles).map((file) => ({
            name: file.name,
            path: '',
            uploading: true,
            deleting: false,
            file,
        }));
        setFiles((prev) => [...prev, ...newFiles]);
        try {
            const uploadedPaths = await uploadFiles(newFiles.map(f => f.file!));
            setFiles((prev) =>
                prev.map((f, idx) =>
                    newFiles.includes(f)
                        ? { ...f, path: uploadedPaths[newFiles.indexOf(f)], uploading: false }
                        : f
                )
            );
            const updatedPaths = [
                ...files.filter((f) => f.path).map((f) => f.path),
                ...uploadedPaths,
            ];
            onChange(arg.name, isMultiFileArgument(arg) ? updatedPaths : uploadedPaths[0] || '');
        } catch {
            setFiles((prev) => prev.filter((f) => !newFiles.includes(f)));
        }
    };

    const handleDelete = async (fileObj: FileItem) => {
        setFiles((prev) =>
            prev.map((f) => (f === fileObj ? { ...f, deleting: true } : f))
        );
        try {
            await deleteFile(fileObj.path);
            const updated = files.filter((f) => f !== fileObj);
            setFiles(updated);
            const updatedPaths = updated.filter((f) => f.path).map((f) => f.path);
            onChange(arg.name, isMultiFileArgument(arg) ? updatedPaths : updatedPaths[0] || '');
        } catch {
            setFiles((prev) =>
                prev.map((f) => (f === fileObj ? { ...f, deleting: false } : f))
            );
        }
    };

    return (
        <Flex direction="column" gap="size-200">
            <FileTrigger
                onSelect={handleFiles}
                allowsMultiple={isMultiFileArgument(arg)}
                acceptedFileTypes={arg.mimeTypes ? arg.mimeTypes : undefined}
            >
                <Button variant="primary">{isMultiFileArgument(arg) ? 'Upload Files' : 'Upload File'}</Button>
            </FileTrigger>
            <ListView aria-label="Uploaded files" selectionMode="none" items={files}>
                {(file) => (
                    <Item key={file.path || file.name}>
                        <Text>{file.name}</Text>
                        {file.uploading ? (
                            <ProgressCircle isIndeterminate size="S" aria-label="Uploading" />
                        ) : file.deleting ? (
                            <ProgressCircle isIndeterminate size="S" aria-label="Deleting" />
                        ) : (
                            <Button variant="negative" isPending={file.deleting} onPress={() => handleDelete(file)} aria-label="Delete file" isDisabled={file.uploading}>
                                <Delete/>
                                <Text>Delete</Text>
                            </Button>
                        )}
                    </Item>
                )}
            </ListView>
        </Flex>
    );
};

export default FileField;