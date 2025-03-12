import { Content, Flex, Grid, Heading, View } from '@adobe/react-spectrum';
import FolderOpen from '@spectrum-icons/workflow/FolderOpen';
import LockClosed from '@spectrum-icons/workflow/LockClosed';
import Download from '@spectrum-icons/workflow/Download';
import Dashboard from '@spectrum-icons/workflow/Dashboard';
import Code from '@spectrum-icons/workflow/Code';
import Star from '@spectrum-icons/workflow/Star';

const HomePage = () => {
    return (
        <Grid
            areas={['welcome newApproach allInOne', 'contentManagement permissionsManagement dataImports']}
            columns={['1fr', '1fr', '1fr']}
            gap="size-200"
            marginY="size-200"
        >
            <View gridArea="welcome" backgroundColor="gray-50" borderWidth="thin" borderColor="dark" borderRadius="medium" padding="size-200" minHeight="size-2400">
                <Flex direction="row" alignItems="center" gap="size-200">
                    <View minWidth="size-800">
                        <Dashboard size="L" />
                    </View>
                    <Flex direction="column" gap="size-100">
                        <Heading level={3}>Hi there!</Heading>
                        <Content>Welcome to AEM Content Management (ACM). Streamline your workflow and enhance productivity with our intuitive interface and powerful features.</Content>
                    </Flex>
                </Flex>
            </View>
            <View gridArea="newApproach" backgroundColor="gray-50" borderWidth="thin" borderColor="dark" borderRadius="medium" padding="size-200" minHeight="size-2400">
                <Flex direction="row" alignItems="center" gap="size-200">
                    <View minWidth="size-800">
                        <Code size="L" />
                    </View>
                    <Flex direction="column" gap="size-100">
                        <Heading level={3}>New Approach</Heading>
                        <Content>Experience a different way of using Groovy scripts. ACM ensures the instance is healthy before scripts decide when to run: once, periodically, or at an exact date and time. Execute scripts in parallel or sequentially, offering a complete change in paradigm. Unlike traditional methods, ACM allows scripts to run at specific moments offering unmatched flexibility and control.</Content>
                    </Flex>
                </Flex>
            </View>
            <View gridArea="allInOne" backgroundColor="gray-50" borderWidth="thin" borderColor="dark" borderRadius="medium" padding="size-200" minHeight="size-2400">
                <Flex direction="row" alignItems="center" gap="size-200">
                    <View minWidth="size-800">
                        <Star size="L" />
                    </View>
                    <Flex direction="column" gap="size-100">
                        <Heading level={3}>All-in-one</Heading>
                        <Content>ACM may be a good alternative to tools like APM, AECU, AEM Groovy Console, and AC Tool. Groovy language is ideal to manage all things in content including permissions. In tools like APM/AC Tool, there is a need to learn custom YAML syntax or languages/grammars. In ACM, you only need Groovy, which almost every Java developer knows! Enjoy a single, painless tool setup in AEM projects with no hooks and POM updates.</Content>
                    </Flex>
                </Flex>
            </View>
            <View gridArea="contentManagement" backgroundColor="gray-50" borderWidth="thin" borderColor="dark" borderRadius="medium" padding="size-200" minHeight="size-2400">
                <Flex direction="row" alignItems="center" gap="size-200">
                    <View minWidth="size-800">
                        <FolderOpen size="L" />
                    </View>
                    <Flex direction="column" gap="size-100">
                        <Heading level={3}>Content Management</Heading>
                        <Content>Effortlessly migrate pages and components between versions. Ensure content integrity and resolve issues with confidence.</Content>
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
                        <Content>Apply JCR permissions dynamically. Manage permissions seamlessly during site creation, blueprinting, and for live copies, language copies, and other AEM-specific replication scenarios.</Content>
                    </Flex>
                </Flex>
            </View>
            <View gridArea="dataImports" backgroundColor="gray-50" borderWidth="thin" borderColor="dark" borderRadius="medium" padding="size-200" minHeight="size-2400">
                <Flex direction="row" alignItems="center" gap="size-200">
                    <View minWidth="size-800">
                        <Download size="L" />
                    </View>
                    <Flex direction="column" gap="size-100">
                        <Heading level={3}>Data Imports &amp; Exports</Heading>
                        <Content>Effortlessly integrate data from external sources into the JCR repository, enhancing content management capabilities. By simplifying data import implementation, ACM allows developers to focus more on developing better components and presenting data effectively, ensuring a user-friendly experience.</Content>
                    </Flex>
                </Flex>
            </View>
        </Grid>
    );
};

export default HomePage;