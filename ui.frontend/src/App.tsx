import { defaultTheme, Flex, Provider, View } from '@adobe/react-spectrum';
import { ToastContainer } from '@react-spectrum/toast';
import { useEffect, useState } from 'react';
import { Outlet } from 'react-router-dom';
import './App.css';
import { AppContext } from './AppContext';
import Footer from './components/Footer';
import Header from './components/Header';
import router from './router';
import { apiRequest } from './utils/api';
import { State } from './utils/api.types';

function App() {
  const [state, setState] = useState<State>({
    healthStatus: {
      healthy: true,
      issues: [],
    },
    instanceSettings: {
      id: 'default',
      timezoneId: 'UTC',
    },
    queuedExecutions: [],
    publish: false,
    cloudVersion: true
  });

  useEffect(() => {
    const fetchState = async () => {
      try {
        const response = await apiRequest<State>({
          operation: 'Fetch state',
          url: '/apps/acm/api/state.json',
          method: 'get',
        });
        setState(response.data.data);
      } catch (error) {
        console.warn('Cannot fetch state:', error);
      }
    };
    fetchState();
    const intervalId = setInterval(fetchState, 5000);
    return () => clearInterval(intervalId);
  }, []);

  return (
    <Provider
      theme={defaultTheme}
      router={router}
      colorScheme={'light'}
      locale={'en-uk'}
      UNSAFE_style={{
        display: 'flex',
        height: '100%',
        flexDirection: 'column',
      }}
    >
      <AppContext.Provider value={state}>
        <Flex direction="column" flex="1">
          <View paddingX="size-200" paddingTop="size-200">
            <Header />
          </View>
          <View paddingX="size-200" flex="1" UNSAFE_style={{ display: 'flex' }}>
            <Outlet />
          </View>
          <View paddingX="size-200" paddingBottom="size-200">
            <Footer />
          </View>
        </Flex>
        <ToastContainer />
      </AppContext.Provider>
    </Provider>
  );
}

export default App;
