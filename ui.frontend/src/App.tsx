import './App.css'
import { HashRouter, Route, Routes, Navigate } from 'react-router-dom'
import {
    defaultTheme, Flex,
    Footer,
    Grid, Link,
    Provider, TextArea,
    View
} from '@adobe/react-spectrum';
import { AppLink } from './AppLink';

function App() {
    return (
        <Provider theme={defaultTheme} colorScheme={"dark"}>
            <HashRouter>
                <View padding="size-200">
                    <Grid
                        areas={[
                            'header  header',
                            'content content',
                            'footer  footer'
                        ]}
                        columns={['1fr', '3fr']}
                        rows={['size-1000', 'auto', 'size-1000']}
                        height={'100vh'}
                        gap="size-100">
                        <View gridArea="header">
                            <Flex direction="row" gap="size-200">
                                <AppLink to="/console">
                                    Console
                                </AppLink>
                                <AppLink to="/scripts">
                                    Scripts
                                </AppLink>
                            </Flex>
                        </View>
                        <View gridArea="content">
                            <Routes>
                                <Route path="/" element={<Navigate to="/console" />} />
                                <Route path="/scripts" element={
                                    <div>
                                        <h2>Scripts</h2>
                                        <p>Manage your migration scripts here.</p>
                                    </div>
                                } />
                                <Route path="/console" element={
                                    <div>
                                        <h2>Console</h2>
                                        <p>View console output here.</p>

                                        <Flex gap="size-150" wrap>
                                            <TextArea
                                                label="Notes"
                                                defaultValue="This is on a wait list" />
                                        </Flex>
                                    </div>
                                } />
                            </Routes>
                        </View>
                        <View gridArea="footer">
                            <Footer><Link href="https://vml.com" target="_blank">VML Enterprise Solutions</Link> &copy; All rights reserved.</Footer>
                        </View>
                    </Grid>
                </View>
            </HashRouter>
        </Provider>
    )
}

export default App
