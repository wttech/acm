import { Flex, LabeledValue, View } from '@adobe/react-spectrum';
import { ScriptMetadata as ScriptMetadataType } from '../types/script';
import Markdown from './Markdown';

type ScriptMetadataProps = {
  metadata: ScriptMetadataType;
};

const ScriptMetadata = ({ metadata }: ScriptMetadataProps) => {
  const entries = Object.entries(metadata);

  if (entries.length === 0) {
    return null;
  }

  const renderEntries = () => {
    const result: JSX.Element[] = [];
    
    entries.forEach(([key, value]) => {
      const label = key.charAt(0).toUpperCase() + key.slice(1);
      
      if (Array.isArray(value)) {
        value.forEach((item) => {
          result.push(
            <LabeledValue 
              key={`${key}-${result.length}`}
              label={label}
              value={<Markdown code={item}/>}
            />
          );
        });
      } else {
        result.push(
          <LabeledValue 
            key={key}
            label={label}
            value={<Markdown code={value} />}
          />
        );
      }
    });
    
    return result;
  };

  return (
    <View 
      backgroundColor="gray-50" 
      padding="size-200" 
      borderRadius="medium" 
      borderColor="dark" 
      borderWidth="thin"
    >
      <Flex direction="column" gap="size-200">
        {renderEntries()}
      </Flex>
    </View>
  );
};

export default ScriptMetadata;
