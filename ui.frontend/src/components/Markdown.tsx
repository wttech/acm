import React from 'react';
import { default as ReactMarkdown } from 'react-markdown';
import rehypeRaw from 'rehype-raw';
import remarkGfm from 'remark-gfm';
import dedent from 'dedent';
import styles from './markdown.module.css';

interface MarkdownProps {
  code: string;
  trimIndent?: boolean;
}

const Markdown: React.FC<MarkdownProps> = ({ code, trimIndent = true }) => {
  const codeDedented = (trimIndent && code) ? dedent(code) : code;
  return (
    <div className={styles.markdown}>
      <ReactMarkdown remarkPlugins={[remarkGfm]} rehypePlugins={[rehypeRaw]} children={codeDedented} />
    </div>
  );
};

export default Markdown;