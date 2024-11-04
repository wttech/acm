import './App.css'
import { HashRouter, Route, Routes, Navigate } from 'react-router-dom'
import {
    Button,
    ButtonGroup, Cell, Column,
    defaultTheme, Flex, Text,
    Footer,
    Grid, Heading, Link,
    Provider, Row, TableBody, TableHeader, TableView,
    View
} from '@adobe/react-spectrum';
import { AppLink } from './AppLink';

import Gears from '@spectrum-icons/workflow/Gears';

import Editor from '@monaco-editor/react';

import groovyScript from './script.groovy';
import { loader } from '@monaco-editor/react';
import Spellcheck from "@spectrum-icons/workflow/Spellcheck";

// Initialize Monaco Editor to be using embedded resources (to avoid CORS/CSP issues)
loader.config({
    paths: {
        vs: process.env.NODE_ENV === 'production' ? '/apps/migrator/spa/js/monaco-editor/vs' : '/node_modules/monaco-editor/min/vs',
    },
});

// read script.example.groovy as string

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
                        <Routes>
                            <Route path="/" element={<Navigate to="/console" />} />
                            <Route path="/scripts" element={
                                <Flex direction="column">
                                    <View>
                                        <Heading>Scripts</Heading>
                                        <p>Manage your migration scripts here.</p>
                                    </View>
                                    <TableView
                                        aria-label="Example table with static contents"
                                        selectionMode="multiple"
                                    >
                                        <TableHeader>
                                            <Column>Name</Column>
                                            <Column>Type</Column>
                                            <Column align="end">Date Modified</Column>
                                        </TableHeader>
                                        <TableBody>
                                            <Row>
                                                <Cell>Games</Cell>
                                                <Cell>File folder</Cell>
                                                <Cell>6/7/2020</Cell>
                                            </Row>
                                            <Row>
                                                <Cell>Program Files</Cell>
                                                <Cell>File folder</Cell>
                                                <Cell>4/7/2021</Cell>
                                            </Row>
                                            <Row>
                                                <Cell>bootmgr</Cell>
                                                <Cell>System file</Cell>
                                                <Cell>11/20/2010</Cell>
                                            </Row>
                                            <Row>
                                                <Cell>log.txt</Cell>
                                                <Cell>Text Document</Cell>
                                                <Cell>1/18/2016</Cell>
                                            </Row>
                                        </TableBody>
                                    </TableView>
                                </Flex>
                            } />
                            <Route path="/console" element={
                                <Flex direction="column" gap="size-200">
                                    <View>
                                        <Heading>Console</Heading>
                                        <p>Write your Groovy script here.</p>
                                    </View>
                                    <View borderWidth="thin"
                                            borderColor="dark"
                                            borderRadius="medium"
                                            padding="size-100">
                                        <Editor theme="vs-dark"
                                                defaultValue={groovyScript}
                                                height="480px"
                                                language="java"
                                        />
                                    </View>
                                    <ButtonGroup>
                                        <Button variant="secondary"><Spellcheck/><Text>Check syntax</Text></Button>
                                        <Button variant="accent"><Gears/><Text>Execute</Text></Button>
                                    </ButtonGroup>
                                </Flex>
                            }/>
                        </Routes>
                    </View>
                    <View gridArea="footer" padding="size-200">
                        <Footer><Link href="https://vml.com" target="_blank">VML Enterprise
                            Solutions</Link> &copy; All rights reserved.</Footer>
                    </View>
                </Grid>
            </HashRouter>
        </Provider>
    )
}

export default App
