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
        <Provider theme={defaultTheme} colorScheme={"light"} locale={"en-uk"} UNSAFE_style={{display: 'flex', height: '100%', flexDirection: 'column'}}>
            <HashRouter>
                <Flex direction="column" flex="1">
                    <View paddingX="size-200" paddingTop="size-200">
                        <Header/>
                    </View>
                    <View paddingX="size-200" flex="1" UNSAFE_style={{ display: 'flex'}}>
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