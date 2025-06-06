import { Button, FileTrigger, Flex, Item, ListView, ProgressCircle, Text } from '@adobe/react-spectrum';
import Delete from '@spectrum-icons/workflow/Delete';
import { useState } from 'react';
import { apiRequest } from '../utils/api';
import { FileOutput } from '../utils/api.types.ts';

interface FileFieldProps {
  value: string | string[];
  onChange: (value: string | string[]) => void;
  allowMultiple: boolean;
  mimeTypes?: string[];
  min?: number;
  max?: number;
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
    headers: {},
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

const FileField: React.FC<FileFieldProps> = ({ value, onChange, mimeTypes, allowMultiple, min, max }) => {
  const [files, setFiles] = useState<FileItem[]>(
    (Array.isArray(value) ? value : value ? [value] : []).map((path) => ({
      name: path.split('/').pop() ?? path,
      path,
      uploading: false,
      deleting: false,
    })),
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
      const uploadedPaths = await uploadFiles(newFiles.map((f) => f.file!));
      setFiles((prev) => prev.map((f) => (newFiles.includes(f) ? { ...f, path: uploadedPaths[newFiles.indexOf(f)], uploading: false } : f)));
      const updatedPaths = [...files.filter((f) => f.path).map((f) => f.path), ...uploadedPaths];
      onChange(allowMultiple ? updatedPaths : uploadedPaths[0] || '');
    } catch {
      setFiles((prev) => prev.filter((f) => !newFiles.includes(f)));
    }
  };

  const handleDelete = async (fileObj: FileItem) => {
    setFiles((prev) => prev.map((f) => (f === fileObj ? { ...f, deleting: true } : f)));
    try {
      await deleteFile(fileObj.path);
      const updated = files.filter((f) => f !== fileObj);
      setFiles(updated);
      const updatedPaths = updated.filter((f) => f.path).map((f) => f.path);
      onChange(allowMultiple ? updatedPaths : updatedPaths[0] || '');
    } catch {
      setFiles((prev) => prev.map((f) => (f === fileObj ? { ...f, deleting: false } : f)));
    }
  };

  return (
    <Flex direction="column" gap="size-200">
      <FileTrigger onSelect={handleFiles} allowsMultiple={allowMultiple} acceptedFileTypes={mimeTypes ? mimeTypes : undefined}>
        <Button width="size-2000" variant="primary">{allowMultiple ? 'Upload Files' : 'Upload File'}</Button>
      </FileTrigger>
      <ListView aria-label="Uploaded files" selectionMode="none" items={files}>
        {(file) => (
          <Item key={file.path || file.name}>
            <Flex direction="row" alignItems="center" gap="size-100">
              {file.uploading ? (
                  <ProgressCircle isIndeterminate size="S" aria-label="Uploading" />
              ) : file.deleting ? (
                  <ProgressCircle isIndeterminate size="S" aria-label="Deleting" />
              ) : (
                  <Button variant="negative" isPending={file.deleting} onPress={() => handleDelete(file)} aria-label="Delete file" isDisabled={file.uploading}>
                    <Delete />
                  </Button>
              )}
              <Text>{file.name}</Text>
            </Flex>
          </Item>
        )}
      </ListView>
    </Flex>
  );
};

export default FileField;
