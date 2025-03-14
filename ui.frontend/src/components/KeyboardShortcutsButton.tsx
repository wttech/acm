import { Button, ButtonGroup, Content, Dialog, DialogTrigger, Divider, Flex, Grid, Heading, Keyboard, Text, View } from '@adobe/react-spectrum';
import Close from '@spectrum-icons/workflow/Close';
import Help from '@spectrum-icons/workflow/Help';
import { Fragment } from 'react';
import { getOsInfo } from '../utils/os.ts';

const KeyCombination = ({ elements, title }: { elements: string[]; title: string }) => (
    <>
        <Keyboard>
            <Flex gap="size-50" alignItems="center">
                {elements.map((el, index) => (
                    <Fragment key={`${elements.join('-')}-${index}`}>
                        <View borderWidth="thin" borderColor="gray-400" borderRadius="regular" UNSAFE_style={{ display: 'inline' }} paddingY="size-25" paddingX="size-50">
                            {el}
                        </View>
                        {index + 1 !== elements.length && '+'}
                    </Fragment>
                ))}
            </Flex>
        </Keyboard>
        <Text alignSelf="center">{title}</Text>
    </>
);

const KeyboardShortcutsButton = () => {
    const { modifier } = getOsInfo();

    return (
        <DialogTrigger>
            <Button variant="secondary" style="fill">
                <Help />
                <Text>Help</Text>
            </Button>
            {(close) => (
                <Dialog>
                    <Heading>Keyboard Shortcuts</Heading>
                    <Divider />
                    <Content>
                        <Grid columns={['min-content', 'auto']} rowGap="size-100" columnGap="size-200" alignContent="center">
                            <KeyCombination elements={['Fn', 'F1']} title="Command&nbsp;Palette" />
                            <KeyCombination elements={[modifier, '.']} title="Quick Fixes" />
                            <KeyCombination elements={['Shift', modifier, 'F']} title="Toggle Focus Mode" />
                            <KeyCombination elements={[modifier, '+']} title="Zoom In" />
                            <KeyCombination elements={[modifier, '-']} title="Zoom Out" />
                        </Grid>
                    </Content>
                    <ButtonGroup>
                        <Button variant="secondary" onPress={close}>
                            <Close size="XS" />
                            <Text>Close</Text>
                        </Button>
                    </ButtonGroup>
                </Dialog>
            )}
        </DialogTrigger>
    );
};

export default KeyboardShortcutsButton;
