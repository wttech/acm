import './App.css'
import {HashRouter} from 'react-router-dom'
import {
    defaultTheme,
    Provider,
    View,
    Flex
} from '@adobe/react-spectrum';

import {ToastContainer} from '@react-spectrum/toast'

import Content from "./components/Content";
import Header from "./components/Header";
import Footer from "./components/Footer";

function App() {
    return (
        <Provider theme={defaultTheme} colorScheme={"light"}>
            <HashRouter>
                <Flex direction="column" height="100vh">
                    <View 
                        flex
                        UNSAFE_style={{ boxSizing: 'border-box' }}
                        padding="size-200"
                    >
                        <Flex
                            direction="column"
                            height="100%"
                            gap="size-100"
                        >
                            <View marginBottom="size-100">
                                <Header/>
                            </View>
                            <View flex>
                                <Content/>
                            </View>
                            <Footer/>
                        </Flex>
                    </View>
                </Flex>
            </HashRouter>
            <ToastContainer/>
        </Provider>
    )
}

export default App
