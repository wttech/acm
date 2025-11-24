import React from 'react';
import { default as ReactMarkdown } from 'react-markdown';
import rehypeRaw from 'rehype-raw';
import remarkGfm from 'remark-gfm';
import { Strings } from '../utils/strings';
import styles from './markdown.module.css';
import Mermaid from './Mermaid';

interface MarkdownProps {
  code: string;
  trimIndent?: boolean;
}

const Markdown: React.FC<MarkdownProps> = ({ code, trimIndent = true }) => {
  const processedCode = trimIndent ? Strings.dedent(code || '') : code || '';

  return (
    <div className={styles.markdown}>
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        rehypePlugins={[rehypeRaw]}
        components={{
          code({ className, children, ...props }) {
            const match = /language-(\w+)/.exec(className || '');
            const language = match ? match[1] : null;

            if (language === 'mermaid') {
              return <Mermaid chart={String(children).trim()} />;
            }

            return (
              <code className={className} {...props}>
                {children}
              </code>
            );
          },
        }}
      >
        {processedCode}
      </ReactMarkdown>
    </div>
  );
};

export default Markdown;
