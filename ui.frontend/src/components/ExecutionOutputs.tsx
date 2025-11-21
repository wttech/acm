import { Badge, Content, ContextualHelp, Flex, Heading, Text, View } from '@adobe/react-spectrum';
import { Field } from '@react-spectrum/label';
import Upload from '@spectrum-icons/workflow/UploadToCloud';
import React from 'react';
import { Objects } from '../utils/objects';
import CodeTextarea from './CodeTextarea';
import SnippetCode from './SnippetCode';

type ExecutionOutputsProps = {
  outputs: Record<string, unknown> | null | undefined;
};

const ExecutionOutputs: React.FC<ExecutionOutputsProps> = ({ outputs }) => {
  const isEmpty = Objects.isEmpty(outputs ?? undefined);
  const count = isEmpty ? 0 : Object.keys(outputs!).length;

  return (
    <Field label={isEmpty ? 'Outputs' : `Outputs (${count})`} width="100%">
      <div style={{ width: '100%' }}>
        {isEmpty ? (
          <Flex alignItems="center" gap="size-100">
            <Badge variant="neutral">
              <Upload />
              <Text>Not generated</Text>
            </Badge>
            <ContextualHelp variant="info">
              <Heading>Generating outputs</Heading>
              <Content>
                <View marginBottom="size-100">
                  <Text>
                    Use <code>doRun()</code> method to return structured data, files, or summaries:
                  </Text>
                </View>
                <SnippetCode
                  content={`void doRun() {
  def report = outputs.file("report") {
    label = "Report"
    description = "Users report as CSV"
    downloadName = "report.csv"
  }
  report.out.println("Name,Surname,Date")
  report.out.println("John,Doe,2024-01-01")
  
  outputs.text("summary") {
    value = "Processed \${count} users"
  }
}`}
                />
                <View marginTop="size-100">
                  <Text>
                    Use <code>outputs.file()</code> for downloadable assets, reports or <code>outputs.text()</code> for summaries and documentation.
                  </Text>
                </View>
              </Content>
            </ContextualHelp>
          </Flex>
        ) : (
          <CodeTextarea language="json" value={JSON.stringify(outputs, null, 2)} options={{ readOnly: true }} />
        )}
      </div>
    </Field>
  );
};

export default ExecutionOutputs;
