import { defaultTheme, Flex, Provider, View } from '@adobe/react-spectrum';
import { ToastContainer } from '@react-spectrum/toast';
import equal from 'fast-deep-equal';
import { useEffect, useRef, useState } from 'react';
import { Outlet } from 'react-router-dom';
import './App.css';
import { AppContext } from './AppContext';
import Footer from './components/Footer';
import Header from './components/Header';
import router from './router';
import { State, StateDefault } from './types/main.ts';
import { apiRequest } from './utils/api';
import { intervalToTimeout } from './utils/spectrum.ts';

function App() {
  const [state, setState] = useState<State>(StateDefault);
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
          timeout: intervalToTimeout(state.spaSettings.appStateInterval),
          quiet: true,
        });
        const stateNew = response.data.data;
        if (!equal(stateNew, state)) {
          setState((prevState) => {
            if (!equal(stateNew, prevState)) {
              return stateNew;
            }
            return prevState;
          });
        }
      } catch (error) {
        console.warn('Cannot fetch application state:', error);
      } finally {
        isFetching.current = false;
      }
    };

    fetchState();
    const intervalId = setInterval(fetchState, state.spaSettings.appStateInterval);
    return () => clearInterval(intervalId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [state.spaSettings.appStateInterval]);

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
