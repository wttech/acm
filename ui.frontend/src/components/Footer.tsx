import { Link, Footer as SpectrumFooter, Divider } from "@adobe/react-spectrum";
import { Flex, View } from "@adobe/react-spectrum";

const Footer = () => {
    return (
        <View>
            <Divider size="S" marginY="size-200" />
            <SpectrumFooter>
                <Flex alignItems="center" gap="size-150" justifyContent="space-between">
                    <Flex alignItems="center" gap="size-150">
                        <Link href="https://www.vml.com" target="_blank">
                            <img src="./vml-logo.svg" alt="VML Logo" width="85" />
                        </Link>
                        <View>
                            <Link href="https://www.vml.com/expertise/enterprise-solutions" target="_blank">Enterprise Solutions</Link>
                            <View marginTop="size-50">&copy; All rights reserved.</View>
                        </View>
                    </Flex>
                    <Link href="https://github.com/wunderman-thompson/wtpl-aem-contentor" target="_blank">
                        <Flex alignItems="center" gap="size-75">
                            <img src="./github-mark.svg" alt="GitHub" width="16" height="16" style={{ color: 'var(--spectrum-global-color-gray-800)' }} />
                            <View>View &apos;Contentor&apos; on GitHub</View>
                        </Flex>
                    </Link>
                </Flex>
            </SpectrumFooter>
        </View>
    );
};

export default Footer;
