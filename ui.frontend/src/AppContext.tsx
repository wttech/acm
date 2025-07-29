import { createContext } from 'react';
import { State } from './utils/api.types';

const AppContext = createContext<State | undefined>(undefined);

export { AppContext };
