import './App.css'
import { HashRouter } from 'react-router-dom'
import {
    defaultTheme, Flex,
    Footer,
    Grid, Link,
    Provider,
    View
} from '@adobe/react-spectrum';
import { AppLink } from './AppLink';

import { loader } from '@monaco-editor/react';
import AppRoutes from "./AppRoutes.tsx";

// Initialize Monaco Editor to be using embedded resources (to avoid CORS/CSP issues)
loader.config({
    paths: {
        vs: process.env.NODE_ENV === 'production' ? '/apps/migrator/spa/js/monaco-editor/vs' : '/node_modules/monaco-editor/min/vs',
    },
});

function App() {
    return (
        <Provider theme={defaultTheme} colorScheme={"dark"}>
            <HashRouter>
                <Grid
                    areas={[
                        'header  header',
                        'content content',
                        'footer  footer'
                    ]}
                    columns={['1fr', '3fr']}
                    rows={['size-1000', 'auto', 'size-1000']}
                    height={'100vh'}>
                    <View gridArea="header" padding="size-200">
                        <Flex direction="row" gap="size-200">
                            <AppLink to="/console">
                                Console
                            </AppLink>
                            <AppLink to="/scripts">
                                Scripts
                            </AppLink>
                        </Flex>
                    </View>
                    <View gridArea="content" padding="size-200">
                        <AppRoutes/>
                    </View>
                    <View gridArea="footer" padding="size-200">
                        <Footer>
                            <Link href="https://vml.com" target="_blank">VML Enterprise
                            Solutions</Link> &copy; All rights reserved.
                        </Footer>
                    </View>
                </Grid>
            </HashRouter>
        </Provider>
    )
}

export default App
