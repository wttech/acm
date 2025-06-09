import { Button, FileTrigger, Flex, Item, ListView, ProgressCircle, Text } from '@adobe/react-spectrum';
import Delete from '@spectrum-icons/workflow/Delete';
import FileAdd from '@spectrum-icons/workflow/FileAdd';
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
  files.forEach((file) => formData.append(file.name, file));
  const response = await apiRequest<FileOutput>({
    operation: 'File upload',
    url: '/apps/acm/api/file.json',
    method: 'POST',
    data: formData,
    headers: {},
  });
  return response.data.data.files;
};

const deleteFile = async (path: string): Promise<string> => {
  const response = await apiRequest<FileOutput>({
    operation: 'File delete',
    url: `/apps/acm/api/file.json?path=${encodeURIComponent(path)}`,
    method: 'DELETE',
  });
  return response.data.data.files[0];
};

const FileUploader: React.FC<FileFieldProps> = ({ value, onChange, mimeTypes, allowMultiple, max }) => {
  const [files, setFiles] = useState<FileItem[]>(
    (Array.isArray(value) ? value : value ? [value] : []).map((path) => ({
      name: path.split('/').pop() ?? path,
      path,
      uploading: false,
      deleting: false,
    })),
  );
  const uploadedCount = files.filter((f) => f.path).length;
  const atMax = typeof max === 'number' && uploadedCount >= max;

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
      setFiles((prev) =>
        prev.map((f) => {
          const idx = newFiles.findIndex((nf) => nf.name === f.name && f.uploading && !f.path);
          if (idx !== -1) {
            return { ...f, path: uploadedPaths[idx], uploading: false };
          }
          return f;
        }),
      );
      const updatedPaths = [...files.filter((f) => f.path).map((f) => f.path), ...uploadedPaths];
      onChange(allowMultiple ? updatedPaths : uploadedPaths[0] || '');
    } catch {
      setFiles((prev) => prev.filter((f) => !newFiles.some((nf) => nf.name === f.name && f.uploading && !f.path)));
    }
  };

  const handleDelete = async (fileObj: FileItem) => {
    setFiles((prev) => prev.map((f) => (f.path === fileObj.path ? { ...f, deleting: true } : f)));
    try {
      await deleteFile(fileObj.path);
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
      {atMax ? (
        <Button width="size-1250" variant="primary" isDisabled>
          <FileAdd />
          <Text>Upload</Text>
        </Button>
      ) : (
        <FileTrigger onSelect={handleFiles} allowsMultiple={allowMultiple} acceptedFileTypes={mimeTypes ? mimeTypes : undefined}>
          <Button width="size-1250" variant="primary">
            <FileAdd />
            <Text>Upload</Text>
          </Button>
        </FileTrigger>
      )}
      {uploadedCount > 0 && (
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
      )}
    </Flex>
  );
};

export default FileUploader;
