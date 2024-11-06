import './App.css'
import {HashRouter} from 'react-router-dom'
import {
    defaultTheme,
    Provider,
    View,
} from '@adobe/react-spectrum';

import {ToastContainer} from '@react-spectrum/toast'

import Content from "./components/Content";
import Header from "./components/Header";
import Footer from "./components/Footer";

function App() {
    return (
        <Provider theme={defaultTheme} colorScheme={"light"} height="100vh">
            <HashRouter>
                <View padding="size-200">
                    <View marginBottom="size-200">
                        <Header/>
                    </View>
                    <View marginY="size-200">
                        <Content/>
                    </View>
                    <View marginTop="size-200">
                        <Footer/>
                    </View>
                </View>
            </HashRouter>
            <ToastContainer/>
        </Provider>
    )
}

export default App
