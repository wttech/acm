import { Flex, ProgressBar } from '@adobe/react-spectrum';
import { PropsWithChildren } from 'react';

type LoadingWrapperProps = PropsWithChildren<{
  loading?: boolean;
}>;

const LoadingWrapper = ({ loading, children }: LoadingWrapperProps) => {
  if (loading) {
    return (
      <Flex direction="column" alignItems="center" justifyContent="center" height="100%">
        <ProgressBar label="Loading…" isIndeterminate />
      </Flex>
    );
  } else {
    return children;
  }
};

export default LoadingWrapper;
