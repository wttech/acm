import { Flex, View } from '@adobe/react-spectrum';
import { ReactNode } from 'react';

type InfoCardProps = {
  children: ReactNode;
};

const InfoCard = ({ children }: InfoCardProps) => {
  return (
    <View 
      backgroundColor="static-white" 
      padding="size-200" 
      borderRadius="medium" 
      borderColor="dark" 
      borderWidth="thin" 
      flex="1"
    >
      <Flex direction="column" gap="size-200">
        {children}
      </Flex>
    </View>
  );
};

export default InfoCard;
