import React from 'react';
import { default as ReactMarkdown } from 'react-markdown';
import rehypeRaw from 'rehype-raw';
import remarkGfm from 'remark-gfm';
import { Strings } from '../utils/strings';
import styles from './markdown.module.css';

interface MarkdownProps {
  code: string;
  trimIndent?: boolean;
}

const Markdown: React.FC<MarkdownProps> = ({ code, trimIndent = true }) => {
  const processedCode = trimIndent ? Strings.dedent(code) : code;

  return (
    <div className={styles.markdown}>
      <ReactMarkdown remarkPlugins={[remarkGfm]} rehypePlugins={[rehypeRaw]} children={processedCode} />
    </div>
  );
};

export default Markdown;
