import { createContext } from 'react';
import { State } from './types/main';

const AppContext = createContext<State | undefined>(undefined);

export { AppContext };
