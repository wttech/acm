import { useEffect } from 'react';
import { useFormContext } from 'react-hook-form';

const useCrossFieldValidation = (currentField: string) => {
    const { watch, trigger, getValues } = useFormContext();

    useEffect(() => {
        const subscription = watch((value, { name }) => {
            if (name === currentField) {
                const otherFields = Object.keys(getValues()).filter(field => field !== currentField);
                trigger(otherFields);
            }
        });
        return () => subscription.unsubscribe();
    }, [watch, trigger, getValues, currentField]);
};

export default useCrossFieldValidation;