import { Badge, Content, ContextualHelp, Flex, Heading, LabeledValue, Link, Text, View } from '@adobe/react-spectrum';
import { Field } from '@react-spectrum/label';
import DataUnavailable from '@spectrum-icons/workflow/DataUnavailable';
import React from 'react';
import { ExecutableMetadata as ExecutableMetadataType } from '../types/executable';
import { Strings } from '../utils/strings';
import Markdown from './Markdown';
import SnippetCode from './SnippetCode';

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
              <Text>Not defined</Text>
            </Badge>
            <ContextualHelp variant="info">
              <Heading>Defining metadata</Heading>
              <Content>
                <View marginBottom="size-100">
                  <Text>
                    Use YAML frontmatter in a block comment. Supports{' '}
                    <Link href="https://github.github.com/gfm/" target="_blank">
                      GFM
                    </Link>{' '}
                    and{' '}
                    <Link href="https://mermaid.js.org/intro/syntax-reference.html" target="_blank">
                      Mermaid
                    </Link>
                    .
                  </Text>
                </View>
                <SnippetCode
                  language="groovy"
                  fontSize="small"
                  content={`/*
---
version: '1.0'
author: john.doe@acme.com
tags: [content, migration]
---
This script demonstrates metadata parsing.

\`\`\`mermaid
graph LR
  A-->B-->C
\`\`\`
*/

void doRun() {
  // code
}`}
                />
                <View marginTop="size-100">
                  <Text>Notice that comment must be followed by a blank line.</Text>
                </View>
              </Content>
            </ContextualHelp>
          </Flex>
        </div>
      </Field>
    );
  }

  return (
    <Flex direction="column" gap="size-200">
      {Object.entries(metadata).map(([key, value]) => {
        const label = Strings.capitalizeWords(key);

        if (key === 'tags' && Array.isArray(value)) {
          return (
            <LabeledValue
              key={key}
              label={label}
              value={
                <Flex gap="size-100" wrap>
                  {value.map((tag, index) => (
                    <Badge key={index} variant="neutral">
                      {String(tag)}
                    </Badge>
                  ))}
                </Flex>
              }
            />
          );
        }

        if (Array.isArray(value)) {
          return value.map((item, index) => <LabeledValue key={`${key}-${index}`} label={label} value={<Markdown code={String(item)} />} />);
        }

        return <LabeledValue key={key} label={label} value={<Markdown code={String(value)} />} />;
      })}
    </Flex>
  );
};

export default ExecutableMetadata;
