import mermaid from 'mermaid';
import { useEffect, useRef } from 'react';

interface MermaidProps {
  chart: string;
}

const Mermaid = ({ chart }: MermaidProps) => {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (ref.current) {
      mermaid.run({
        nodes: [ref.current],
      });
    }
  }, [chart]);

  return (
    <div ref={ref} className="mermaid">
      {chart}
    </div>
  );
};

export default Mermaid;
