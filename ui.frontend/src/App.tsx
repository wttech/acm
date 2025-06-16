import { defaultTheme, Flex, Provider, View } from '@adobe/react-spectrum';
import { ToastContainer } from '@react-spectrum/toast';
import equal from 'fast-deep-equal';
import { useEffect, useRef } from 'react';
import { Outlet } from 'react-router-dom';
import './App.css';
import Footer from './components/Footer';
import Header from './components/Header';
import { appState } from './hooks/app.ts';
import router from './router';
import { apiRequest } from './utils/api';
import { State } from './utils/api.types';
import { intervalToTimeout } from './utils/spectrum.ts';

function App() {
  const isFetching = useRef(false);

  useEffect(() => {
    const fetchState = async () => {
      if (isFetching.current) {
        return; // no overlaps
      }
      isFetching.current = true;

      try {
        const response = await apiRequest<State>({
          operation: 'Fetch application state',
          url: '/apps/acm/api/state.json',
          method: 'get',
          timeout: intervalToTimeout(appState.value.spaSettings.appStateInterval),
          quiet: true,
        });
        const stateNew = response.data.data;
        if (!equal(stateNew, appState.value)) {
          appState.value = stateNew;
        }
      } catch (error) {
        console.warn('Cannot fetch application state:', error);
      } finally {
        isFetching.current = false;
      }
    };

    fetchState();
    const intervalId = setInterval(fetchState, appState.value.spaSettings.appStateInterval);
    return () => clearInterval(intervalId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [appState.value.spaSettings.appStateInterval]);

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
    </Provider>
  );
}

export default App;
