import React from 'react';
import { TextField, Checkbox, NumberField, Picker, Item, View } from '@adobe/react-spectrum';
import {
    Argument,
    ArgumentValue,
    isToggleArgument,
    isStringArgument,
    isTextArgument,
    isSelectArgument,
    isNumberArgument,
    ArgumentType
} from '../utils/api.types.ts';
import { Editor } from '@monaco-editor/react';
import {Field} from '@react-spectrum/label';

interface CodeInputProps {
    arg: Argument<ArgumentValue>;
    value: ArgumentValue;
    onChange: (name: string, value: ArgumentValue) => void;
}

const CodeInput: React.FC<CodeInputProps> = ({ arg, value, onChange }) => {
    if (isToggleArgument(arg)) {
        return (
            <View key={arg.name} marginBottom="size-200">
                <Checkbox
                    isSelected={value as boolean}
                    onChange={(val) => onChange(arg.name, val)}
                >
                    {arg.label || arg.name}
                </Checkbox>
            </View>
        );
    } else if (isStringArgument(arg)) {
        return (
            <View key={arg.name} marginBottom="size-200">
                <TextField
                    label={arg.label || arg.name}
                    value={value?.toString() || ''}
                    onChange={(val) => onChange(arg.name, val)}
                />
            </View>
        );
    } else if (isTextArgument(arg)) {
        return (
            <View key={arg.name} marginY="size-100">
                {arg.language ? (
                    <>
                        <Field label={arg.label || arg.name} width="100%">
                            <div>
                                <Editor
                                    language={arg.language}
                                    theme="vs-dark"
                                    height="200px"
                                    options={{scrollBeyondLastLine: false}}
                                    value={value?.toString() || ''}
                                    onChange={(val) => onChange(arg.name, val || '')}
                                />
                            </div>
                        </Field>
                    </>
                ) : (
                    <TextField
                        label={arg.label || arg.name}
                        value={value?.toString() || ''}
                        onChange={(val) => onChange(arg.name, val)}
                    />
                )}
            </View>
        );
    } else if (isSelectArgument(arg)) {
        return (
            <View key={arg.name} marginBottom="size-200">
                <Picker
                    label={arg.label || arg.name}
                    selectedKey={value?.toString() || ''}
                    onSelectionChange={(val) => onChange(arg.name, val)}
                >
                    {Object.entries(arg.options).map(([label, val]) => (
                        <Item key={val?.toString()}>{label}</Item>
                    ))}
                </Picker>
            </View>
        );
    } else if (isNumberArgument(arg)) {
        return (
            <View key={arg.name} marginBottom="size-200">
                <NumberField
                    label={arg.label || arg.name}
                    value={value as number}
                    onChange={(val) => onChange(arg.name, val)}
                    minValue={arg.min}
                    maxValue={arg.max}
                    hideStepper={arg.type === ArgumentType.DOUBLE}
                    formatOptions={arg.type === ArgumentType.INTEGER ? { maximumFractionDigits: 0 } : undefined}
                />
            </View>
        );
    } else {
        return null;
    }
};

export default CodeInput;