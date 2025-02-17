import { Link, StatusLight, Tooltip, TooltipTrigger } from '@adobe/react-spectrum';
import { SyntaxError } from './ImmersiveEditor';

type CompilationStatusProps = {
  isCompiling: boolean;
  compilationError?: string;
  syntaxError?: SyntaxError;
  onCompilationErrorClick: () => void;
};

const CompilationStatus = ({ isCompiling, syntaxError, compilationError, onCompilationErrorClick }: CompilationStatusProps) => {
  if (isCompiling) {
    return (
      <StatusLight variant="yellow" isDisabled>
        Compiling...
      </StatusLight>
    );
  }

  if (syntaxError) {
    return <StatusLight variant="negative">Syntax error</StatusLight>;
  }

  if (compilationError) {
    return (
      <StatusLight variant="negative">
        <TooltipTrigger delay={0}>
          <Link isQuiet onPress={onCompilationErrorClick}>
            Compilation error
          </Link>
          <Tooltip>Click to see details</Tooltip>
        </TooltipTrigger>
      </StatusLight>
    );
  }

  return <StatusLight variant="positive">Success</StatusLight>;
};

export default CompilationStatus;
