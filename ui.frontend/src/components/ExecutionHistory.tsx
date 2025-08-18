import { Button, Flex, Text, View } from '@adobe/react-spectrum';
import DataRemove from "@spectrum-icons/workflow/DataRemove";
import React from "react";

const ExecutionHistory = () => {
  return (
    <Flex direction="column" flex="1" gap="size-200" marginY="size-100">
      <View>
        <Flex direction="row" justifyContent="space-between" alignItems="center">
          <Flex flex="1" alignItems="center">
            <Button variant="negative">
              <DataRemove />
              <Text>Clear</Text>
            </Button>
          </Flex>
          <Flex flex="1" justifyContent="center" alignItems="center">
            &nbsp;
          </Flex>
          <Flex flex="1" justifyContent="end" alignItems="center">
            &nbsp;
          </Flex>
        </Flex>
      </View>
    </Flex>
  );
};

export default ExecutionHistory;
