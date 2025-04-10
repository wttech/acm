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

const AppStateFetchInterval = 2000;

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
  });

  useEffect(() => {
    let isFetching = false; // prevent overlapping

    const fetchState = async () => {
      if (isFetching) {
        return; // skip when already in progress
      }
      isFetching = true;

      const controller = new AbortController();
      const timeout = setTimeout(() => controller.abort(), AppStateFetchInterval - 200);

      try {
        const response = await apiRequest<State>({
          operation: 'Fetch state',
          url: '/apps/acm/api/state.json',
          method: 'get',
          signal: controller.signal,
        });
        setState(response.data.data);
      } catch (error) {
        if (error instanceof Error && error.name === 'AbortError') {
          console.warn('Application state request timed out!', error);
        } else {
          console.warn('Application state cannot be fetched:', error);
        }
      } finally {
        clearTimeout(timeout);
        isFetching = false;
      }
    };

    const intervalId = setInterval(fetchState, AppStateFetchInterval);

    return () => {
      clearInterval(intervalId);
    };
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