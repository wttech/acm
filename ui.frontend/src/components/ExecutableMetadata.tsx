import { Badge, Content, ContextualHelp, Flex, Heading, LabeledValue, View } from '@adobe/react-spectrum';
import { Field } from '@react-spectrum/label';
import DataUnavailable from '@spectrum-icons/workflow/DataUnavailable';
import { Text } from '@adobe/react-spectrum';
import React from 'react';
import { ExecutableMetadata as ExecutableMetadataType } from '../types/executable';
import Markdown from './Markdown';

type ExecutableMetadataProps = {
  metadata: ExecutableMetadataType | null | undefined;
};

const ExecutableMetadata: React.FC<ExecutableMetadataProps> = ({ metadata }) => {
  if (!metadata || Object.keys(metadata).length === 0) {
    return (
      <Field label="Metadata">
        <div>
          <Flex alignItems="center" gap="size-100">
            <Badge variant="neutral">
              <DataUnavailable />
              <Text>Not available</Text>
            </Badge>
            <ContextualHelp variant="info">
              <Heading>Defining metadata</Heading>
              <Content>
                <View marginBottom="size-100">
                  <Text>Use a JavaDoc or GroovyDoc comment block at the top of your script file:</Text>
                </View>
                <pre><small>
                  {`/**
 * Explain purpose here
 *
 * @author Your Name <your.email@example.com>
 * @version 1.0
 */`}
                </small></pre>
                <View marginTop="size-100">
                  <Text>The comment must be followed by a blank line. Description is extracted from text before any @tags.</Text>
                </View>
              </Content>
            </ContextualHelp>
          </Flex>
        </div>
      </Field>
    );
  }

  return (
    <>
      {Object.entries(metadata).map(([key, value]) => {
        const label = key.charAt(0).toUpperCase() + key.slice(1);

        if (Array.isArray(value)) {
          return value.map((item, index) => (
            <LabeledValue key={`${key}-${index}`} label={label} value={<Markdown code={item} />} />
          ));
        }

        return <LabeledValue key={key} label={label} value={<Markdown code={value} />} />;
      })}
    </>
  );
};

export default ExecutableMetadata;
