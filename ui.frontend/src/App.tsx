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
                    <View paddingX="size-200" paddingTop="size-200">
                        <View marginBottom="size-100">
                            <Header/>
                        </View>
                    </View>
                    <View flex paddingX="size-200">
                        <Content/>
                    </View>
                    <View paddingX="size-200" paddingBottom="size-200">
                        <Footer/>
                    </View>
                </Flex>
            </HashRouter>
            <ToastContainer/>
        </Provider>
    )
}

export default App
