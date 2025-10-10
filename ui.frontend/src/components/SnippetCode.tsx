import hljs from 'highlight.js';
import groovy from 'highlight.js/lib/languages/groovy';
import 'highlight.js/styles/vs2015.min.css';
import React, { useEffect, useRef } from 'react';
import './SnippetCode.css';

hljs.registerLanguage('snippet-code-groovy', function (hljs) {
  const groovyLang = groovy(hljs);
  const placeholder = {
    className: 'variable',
    begin: /\$\{\d+(:[^}]+)?}/,
  };

  const stringWithPlaceholders = {
    className: 'string',
    variants: [
      {
        begin: /"/,
        end: /"/,
        contains: [placeholder],
      },
      {
        begin: /'/,
        end: /'/,
        contains: [placeholder],
      },
    ],
  };

  groovyLang.contains.push(placeholder);
  groovyLang.contains = groovyLang.contains.map((mode) => {
    if (mode.className === 'string') {
      return stringWithPlaceholders;
    }
    return mode;
  });

  return groovyLang;
});

interface SnippetCodeProps {
  content: string;
}

const SnippetCode: React.FC<SnippetCodeProps> = ({ content }) => {
  const codeRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (codeRef.current) {
      const codeElement = codeRef.current.querySelector('code');
      if (codeElement) {
        // Clear previous highlighting by resetting content and removing data attribute
        codeElement.textContent = content;
        delete codeElement.dataset.highlighted;
        codeElement.className = 'snippet-code-groovy';
        // Then highlight
        hljs.highlightElement(codeElement);
      }
    }
  }, [content]);

  return (
    <div ref={codeRef}>
      <code className="snippet-code-groovy">{content}</code>
    </div>
  );
};

export default SnippetCode;
