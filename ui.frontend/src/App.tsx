import './App.css'
import {HashRouter} from 'react-router-dom'
import {
    defaultTheme,
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
        <Provider theme={defaultTheme} colorScheme={"light"} height="100vh">
            <HashRouter>
                <View padding="size-100">
                    <View backgroundColor="default" height="size-600" paddingX="size-200" borderBottomWidth="thin" borderBottomColor="light">
                        <Header/>
                    </View>
                    <View padding="size-200">
                        <Content/>
                    </View>
                    <View padding="size-200">
                        <Footer/>
                    </View>
                </View>
            </HashRouter>
        </Provider>
    )
}

export default App
