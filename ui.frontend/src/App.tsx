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
        <Provider theme={defaultTheme} colorScheme={"light"}>
            <HashRouter>
                <View 
                    height="100vh" 
                    UNSAFE_style={{ 
                        display: 'flex', 
                        flexDirection: 'column',
                        boxSizing: 'border-box'
                    }} 
                    padding="size-200"
                >
                    <View marginBottom="size-200">
                        <Header/>
                    </View>
                    <View marginY="size-200" flex={1}>
                        <Content/>
                    </View>
                    <View>
                        <Footer/>
                    </View>
                </View>
            </HashRouter>
            <ToastContainer/>
        </Provider>
    )
}

export default App
