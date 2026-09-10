import { Divider, Flex, Link, Footer as SpectrumFooter, Text, View } from '@adobe/react-spectrum';
import { Urls } from '../utils/url';
import ThreeColumnBar from './ThreeColumnBar';

const Footer = () => {
  const currentYear = new Date().getFullYear();
  const yearText = currentYear === 2024 ? '2024' : `2024 - ${currentYear}`;

  return (
    <View>
      <Divider size="S" marginY="size-200" />
      <SpectrumFooter>
        <ThreeColumnBar
          left={
            <Link href="https://enterprisesolutions.wpp.com/" target="_blank">
              <img src={Urls.asset('wpp-es-primary-navy.svg')} alt="WPP Enterprise Solutions" width="220" />
            </Link>
          }
          center={
            <Text>
              Copyright {yearText} &copy; Licensed under the Apache License, Version 2.0.
            </Text>
          }
          right={
            <Link href="https://github.com/wttech/acm" target="_blank">
              <Flex alignItems="center" gap="size-75">
                <img src={Urls.asset('github-mark.svg')} alt="GitHub" width="16" height="16" style={{ color: 'var(--spectrum-global-color-gray-800)' }} />
                <View>View &apos;Content Manager&apos; on GitHub</View>
              </Flex>
            </Link>
          }
        />
      </SpectrumFooter>
    </View>
  );
};

export default Footer;
