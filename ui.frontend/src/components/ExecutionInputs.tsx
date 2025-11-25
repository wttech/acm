import { Badge, Content, ContextualHelp, Flex, Heading, Text, View } from '@adobe/react-spectrum';
import { Field } from '@react-spectrum/label';
import Download from '@spectrum-icons/workflow/Download';
import React from 'react';
import { Objects } from '../utils/objects';
import CodeTextarea from './CodeTextarea';
import SnippetCode from './SnippetCode';

type ExecutionInputsProps = {
  inputs: Record<string, unknown> | null | undefined;
};

const ExecutionInputs: React.FC<ExecutionInputsProps> = ({ inputs }) => {
  const isEmpty = Objects.isEmpty(inputs ?? undefined);
  const count = isEmpty ? 0 : Object.keys(inputs!).length;

  return (
    <Field label={isEmpty ? 'Inputs' : `Inputs (${count})`} width="100%">
      <div style={{ width: '100%' }}>
        {isEmpty ? (
          <Flex alignItems="center" gap="size-100">
            <Badge variant="neutral">
              <Download />
              <Text>Not described</Text>
            </Badge>
            <ContextualHelp variant="info">
              <Heading>Defining inputs</Heading>
              <Content>
                <View marginBottom="size-100">
                  <Text>
                    Use <code>describeRun()</code> method to collect values from users before execution:
                  </Text>
                </View>
                <SnippetCode
                  language="groovy"
                  fontSize="small"
                  scrollable
                  content={`void describeRun() {
  inputs.path("pagePath") { 
    rootPathExclusive = '/content' 
  }
  inputs.file("pageThumbnailFile") { 
    mimeTypes = ["image/jpeg"] 
  }
  inputs.integerNumber("count") { 
    label = "Users to generate"
    min = 1
    value = 10000 
  }
}`}
                />
                <View marginTop="size-100">
                  <Text>
                    Access input values in <code>doRun()</code> using <code>inputs.value("name")</code>. Supports various types: string, integerNumber, decimalNumber, bool, date, time, path, file, and more.
                  </Text>
                </View>
              </Content>
            </ContextualHelp>
          </Flex>
        ) : (
          <CodeTextarea language="json" value={JSON.stringify(inputs, null, 2)} options={{ readOnly: true, ariaLabel: 'Execution Inputs JSON' }} />
        )}
      </div>
    </Field>
  );
};

export default ExecutionInputs;
