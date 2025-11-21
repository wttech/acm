import hljs from 'highlight.js';
import bash from 'highlight.js/lib/languages/bash';
import groovy from 'highlight.js/lib/languages/groovy';
import java from 'highlight.js/lib/languages/java';
import javascript from 'highlight.js/lib/languages/javascript';
import json from 'highlight.js/lib/languages/json';
import typescript from 'highlight.js/lib/languages/typescript';
import xml from 'highlight.js/lib/languages/xml';
import yaml from 'highlight.js/lib/languages/yaml';
import 'highlight.js/styles/vs2015.min.css';
import React, { useEffect, useRef } from 'react';
import './SnippetCode.css';

// Register standard languages
hljs.registerLanguage('bash', bash);
hljs.registerLanguage('java', java);
hljs.registerLanguage('javascript', javascript);
hljs.registerLanguage('json', json);
hljs.registerLanguage('typescript', typescript);
hljs.registerLanguage('xml', xml);
hljs.registerLanguage('yaml', yaml);

// Register custom groovy variant with placeholder support
hljs.registerLanguage('groovy', function (hljs) {
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
  language: 'groovy' | 'java' | 'javascript' | 'typescript' | 'json' | 'xml' | 'yaml' | 'bash';
  fontSize?: 'small' | 'medium';
}

const SnippetCode: React.FC<SnippetCodeProps> = ({ content, language, fontSize = 'medium' }) => {
  const codeRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (codeRef.current) {
      const codeElement = codeRef.current.querySelector('code');
      if (codeElement) {
        // Clear previous highlighting by resetting content and removing data attribute
        codeElement.textContent = content;
        delete codeElement.dataset.highlighted;
        codeElement.className = `snippet-code snippet-code-${language} snippet-code-${fontSize}`;
        // Then highlight
        hljs.highlightElement(codeElement);
      }
    }
  }, [content, language, fontSize]);

  return (
    <div ref={codeRef} className="snippet-code-wrapper">
      <code className={`snippet-code snippet-code-${language} snippet-code-${fontSize}`}>{content}</code>
    </div>
  );
};

export default SnippetCode;
