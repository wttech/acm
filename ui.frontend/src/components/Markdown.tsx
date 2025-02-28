import React from 'react';
import { default as ReactMarkdown } from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeRaw from 'rehype-raw';
import './markdown.css';

interface MarkdownProps {
    children: string;
}

const Markdown: React.FC<MarkdownProps> = ({ children }) => {
    return (
        <div className="markdown">
            <ReactMarkdown remarkPlugins={[remarkGfm]} rehypePlugins={[rehypeRaw]}>
                {children}
            </ReactMarkdown>
        </div>
    );
};

export default Markdown;