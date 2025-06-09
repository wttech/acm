import { Button, FileTrigger, Flex, Item, ListView, ProgressCircle, Text } from '@adobe/react-spectrum';
import Delete from '@spectrum-icons/workflow/Delete';
import FileAdd from '@spectrum-icons/workflow/FileAdd';
import { useState } from 'react';
import { toastRequest } from '../utils/api';
import { FileOutput } from '../utils/api.types.ts';

interface FileFieldProps {
  value: string | string[];
  onChange: (value: string | string[]) => void;
  allowMultiple: boolean;
  mimeTypes?: string[];
  min?: number;
  max?: number;
}

const generateId = () => `${Date.now()}-${Math.random().toString(36).substring(2, 11)}`;

type FileItem = {
  id: string;
  name: string;
  path: string;
  uploading: boolean;
  deleting: boolean;
  file?: File;
};

const uploadFiles = async (files: File[]): Promise<string[]> => {
  const formData = new FormData();
  files.forEach((file) => formData.append(file.name, file));
  const response = await toastRequest<FileOutput>({
    operation: 'File upload',
    positive: false,
    url: '/apps/acm/api/file.json',
    method: 'POST',
    data: formData,
    headers: {},
  });
  return response.data.data.files;
};

const deleteFiles = async (paths: string[]): Promise<string[]> => {
  const query = paths.map((p) => `path=${encodeURIComponent(p)}`).join('&');
  const response = await toastRequest<FileOutput>({
    operation: 'File delete',
    positive: false,
    url: `/apps/acm/api/file.json?${query}`,
    method: 'DELETE',
  });
  return response.data.data.files;
};

const FileUploader: React.FC<FileFieldProps> = ({ value, onChange, mimeTypes, allowMultiple, max }) => {
  const effectiveMax = allowMultiple ? max : 1;
  const [files, setFiles] = useState<FileItem[]>(
    (Array.isArray(value) ? value : value ? [value] : []).map((path) => ({
      id: generateId(),
      name: path.split('/').pop() ?? path,
      path,
      uploading: false,
      deleting: false,
    })),
  );
  const uploadedCount = files.filter((f) => f.path).length;
  const atMax = typeof effectiveMax === 'number' && uploadedCount >= effectiveMax;

  const handleFiles = async (selectedFiles: FileList | null) => {
    if (!selectedFiles) {
      return;
    }
    const uploadedCount = files.filter((f) => f.path).length;
    const remaining = typeof effectiveMax === 'number' ? Math.max(0, effectiveMax - uploadedCount) : selectedFiles.length;
    const filesToUpload = Array.from(selectedFiles).slice(0, remaining);

    const newFiles: FileItem[] = filesToUpload.map((file) => ({
      id: generateId(),
      name: file.name,
      path: '',
      uploading: true,
      deleting: false,
      file,
    }));

    setFiles((prev) => [...prev, ...newFiles]); // Pre-render all loading indicators

    for (const fileItem of newFiles) {
      try {
        const [uploadedPath] = await uploadFiles([fileItem.file!]);
        setFiles((prev) => prev.map((f) => (f.id === fileItem.id ? { ...f, path: uploadedPath, uploading: false } : f)));
      } catch {
        setFiles((prev) => prev.filter((f) => f.id !== fileItem.id));
      }
    }

    setFiles((prev) => {
      const updatedPaths = prev.filter((f) => f.path).map((f) => f.path);
      onChange(allowMultiple ? updatedPaths : updatedPaths[0] || '');
      return prev;
    });
  };

  const handleDelete = async (fileObj: FileItem) => {
    setFiles((prev) => prev.map((f) => (f.path === fileObj.path ? { ...f, deleting: true } : f)));
    try {
      await deleteFiles([fileObj.path]);
      setFiles((prev) => {
        const updated = prev.filter((f) => f.path !== fileObj.path);
        const updatedPaths = updated.filter((f) => f.path).map((f) => f.path);
        onChange(allowMultiple ? updatedPaths : updatedPaths[0] || '');
        return updated;
      });
    } catch {
      setFiles((prev) => prev.map((f) => (f.path === fileObj.path ? { ...f, deleting: false } : f)));
    }
  };

  return (
    <Flex direction="column" gap="size-200">
      {((allowMultiple && !atMax) || (!allowMultiple && files.length === 0)) && (
        <FileTrigger onSelect={handleFiles} allowsMultiple={allowMultiple} acceptedFileTypes={mimeTypes}>
          <Button width="size-1250" variant="primary">
            <FileAdd />
            <Text>Upload</Text>
          </Button>
        </FileTrigger>
      )}
      {files.length > 0 && (
        <ListView aria-label="Uploaded files" selectionMode="none" items={files}>
          {(file) => (
            <Item key={file.id}>
              <Flex direction="row" alignItems="center" gap="size-100">
                {file.uploading ? (
                  <ProgressCircle isIndeterminate size="M" aria-label="Uploading" />
                ) : file.deleting ? (
                  <ProgressCircle isIndeterminate size="M" aria-label="Deleting" />
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
      )}
    </Flex>
  );
};

export default FileUploader;
