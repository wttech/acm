import { Flex, View } from '@adobe/react-spectrum';
import { ReactNode } from 'react';

type InfoCardProps = {
  children: ReactNode;
  id?: string;
};

const InfoCard = ({ children, id }: InfoCardProps) => {
  return (
    <View id={id} backgroundColor="static-white" padding="size-200" borderRadius="medium" borderColor="dark" borderWidth="thin" flex="1">
      <Flex direction="column" gap="size-200">
        {children}
      </Flex>
    </View>
  );
};

export default InfoCard;
