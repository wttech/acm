import { Link, StatusLight, Text } from '@adobe/react-spectrum';
import { SyntaxError } from './ImmersiveEditor';

type CompilationStatusProps = {
  isCompiling: boolean;
  compilationError?: string;
  syntaxError?: SyntaxError;
  onCompilationErrorClick: () => void;
};

const CompilationStatus = ({ isCompiling, syntaxError, compilationError, onCompilationErrorClick }: CompilationStatusProps) => {
  if (isCompiling) {
    return <StatusLight variant="yellow" isDisabled>Compilation in progress</StatusLight>;
  }
  if (syntaxError) {
    return <StatusLight variant="negative">Compilation failed &mdash; Syntax error</StatusLight>;
  }

  if (compilationError) {
    return (
      <StatusLight variant="negative">
        <Text>Compilation failed</Text>
        <Text>&nbsp;&mdash;&nbsp;</Text>
        <Link isQuiet onPress={onCompilationErrorClick}>See error</Link>
      </StatusLight>
    );
  }

  return <StatusLight variant="positive">Compilation succeeded</StatusLight>;
};

export default CompilationStatus;
