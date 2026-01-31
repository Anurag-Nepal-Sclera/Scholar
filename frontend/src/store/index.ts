import { configureStore, combineReducers } from '@reduxjs/toolkit';
import {
  persistStore,
  persistReducer,
  FLUSH,
  REHYDRATE,
  PAUSE,
  PERSIST,
  PURGE,
  REGISTER,
} from 'redux-persist';
import storage from 'redux-persist/lib/storage';

import authReducer from './slices/authSlice';
import tenantReducer from './slices/tenantSlice';
import cvReducer from './slices/cvSlice';
import matchReducer from './slices/matchSlice';
import campaignReducer from './slices/campaignSlice';
import smtpReducer from './slices/smtpSlice';
import uiReducer from './slices/uiSlice';

const rootReducer = combineReducers({
  auth: authReducer,
  tenant: tenantReducer,
  cv: cvReducer,
  match: matchReducer,
  campaign: campaignReducer,
  smtp: smtpReducer,
  ui: uiReducer,
});

const persistConfig = {
  key: 'scholar-root',
  version: 1,
  storage,
  whitelist: ['auth', 'tenant'], // Only persist auth and tenant state
};

const persistedReducer = persistReducer(persistConfig, rootReducer);

export const store = configureStore({
  reducer: persistedReducer,
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware({
      serializableCheck: {
        ignoredActions: [FLUSH, REHYDRATE, PAUSE, PERSIST, PURGE, REGISTER],
      },
    }),
  devTools: import.meta.env.DEV,
});

export const persistor = persistStore(store);

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
