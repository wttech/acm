import { Content, Flex, Grid, Heading, View } from '@adobe/react-spectrum';
import FolderOpen from '@spectrum-icons/workflow/FolderOpen';
import LockClosed from '@spectrum-icons/workflow/LockClosed';
import Download from '@spectrum-icons/workflow/Download';

const HomePage = () => {
    return (
        <Grid
            areas={['contentManagement permissionsManagement dataImports']}
            columns={['1fr', '1fr', '1fr']}
            gap="size-200"
            marginY="size-200"
        >
            <View gridArea="contentManagement" backgroundColor="gray-50" borderWidth="thin" borderColor="dark" borderRadius="medium" padding="size-200" minHeight="size-2400">
                <Flex direction="row" alignItems="center" gap="size-200">
                    <View minWidth="size-800">
                        <FolderOpen size="L" />
                    </View>
                    <Flex direction="column" gap="size-100">
                        <Heading level={3}>Content Management</Heading>
                        <Content>Migrate pages, components from one version to the recently developed one with ease. Fix the problematic content with sureness.</Content>
                    </Flex>
                </Flex>
            </View>
            <View gridArea="permissionsManagement" backgroundColor="gray-50" borderWidth="thin" borderColor="dark" borderRadius="medium" padding="size-200" minHeight="size-2400">
                <Flex direction="row" alignItems="center" gap="size-200">
                    <View minWidth="size-800">
                        <LockClosed size="L" />
                    </View>
                    <Flex direction="column" gap="size-100">
                        <Heading level={3}>Permissions Management</Heading>
                        <Content>Dynamically apply JCR permissions to content any time. When creating sites from blueprint, making live & language copies, etc.</Content>
                    </Flex>
                </Flex>
            </View>
            <View gridArea="dataImports" backgroundColor="gray-50" borderWidth="thin" borderColor="dark" borderRadius="medium" padding="size-200" minHeight="size-2400">
                <Flex direction="row" alignItems="center" gap="size-200">
                    <View minWidth="size-800">
                        <Download size="L" />
                    </View>
                    <Flex direction="column" gap="size-100">
                        <Heading level={3}>Perform data imports</Heading>
                        <Content>Read 3rd party data from external endpoints and store them in JCR repository then integrate. Importing data hasn't been so ease as before!</Content>
                    </Flex>
                </Flex>
            </View>
        </Grid>
    );
};

export default HomePage;