import { Flex, ProgressCircle, View } from '@adobe/react-spectrum';
import { PropsWithChildren } from 'react';

type LoadingWrapperProps = PropsWithChildren<{
  isRefreshing?: boolean;
}>;

const LoadingWrapper = ({ isRefreshing, children }: LoadingWrapperProps) => {
  if (isRefreshing) {
    return (
      <View position="relative">
        <Flex position="absolute" alignItems="center" height="100%" width="100%" justifyContent="center" zIndex={5}>
          <ProgressCircle isIndeterminate />
        </Flex>
        <View backgroundColor="static-white" UNSAFE_style={{ opacity: 0.5 }}>
          {children}
        </View>
      </View>
    );
  }

  return children;
};

export default LoadingWrapper;
