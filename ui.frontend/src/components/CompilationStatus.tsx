import { Link, StatusLight, Text } from '@adobe/react-spectrum';
import { SyntaxError } from '../hooks/code';

type CompilationStatusProps = {
  compiling: boolean;
  compileError?: string;
  syntaxError?: SyntaxError;
  onErrorClick: () => void;
};

const CompilationStatus = ({ compiling, syntaxError, compileError, onErrorClick }: CompilationStatusProps) => {
  if (compiling) {
    return (
      <StatusLight id="compilation-status" variant="yellow" isDisabled>
        Compilation in progress
      </StatusLight>
    );
  }
  if (syntaxError) {
    return <StatusLight id="compilation-status" variant="negative">Compilation failed &mdash; Syntax error</StatusLight>;
  }

  if (compileError) {
    return (
      <StatusLight id="compilation-status" variant="negative">
        <Text>Compilation failed</Text>
        <Text>&nbsp;&mdash;&nbsp;</Text>
        <Link isQuiet onPress={onErrorClick}>
          See error
        </Link>
      </StatusLight>
    );
  }

  return <StatusLight id="compilation-status" variant="positive">Compilation succeeded</StatusLight>;
};

export default CompilationStatus;
