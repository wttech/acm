import './App.css'
import {HashRouter} from 'react-router-dom'
import {
    defaultTheme,
    Grid,
    Provider,
    View
} from '@adobe/react-spectrum';

import { loader } from '@monaco-editor/react';
import Content from "./components/Content";
import Header from "./components/Header";
import Footer from "./components/Footer";

// Initialize Monaco Editor to be using embedded resources (to avoid CORS/CSP issues)
loader.config({
    paths: {vs: process.env.NODE_ENV === 'production' ? '/apps/migrator/spa/js/monaco-editor/vs' : '/node_modules/monaco-editor/min/vs'},
});

function App() {
    return (
        <Provider theme={defaultTheme} colorScheme={"light"}>
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
                    <View gridArea="header" padding="size-200" backgroundColor="gray-200" height="size-400">
                        <Header/>
                    </View>
                    <View gridArea="content" padding="size-200">
                        <Content/>
                    </View>
                    <View gridArea="footer" padding="size-200">
                        <Footer/>
                    </View>
                </Grid>
            </HashRouter>
        </Provider>
    )
}

export default App
