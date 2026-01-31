import { createSlice, PayloadAction } from '@reduxjs/toolkit';

interface ModalState {
  isOpen: boolean;
  type: string | null;
  data?: unknown;
}

interface UIState {
  sidebarOpen: boolean;
  modal: ModalState;
  globalLoading: boolean;
}

const initialState: UIState = {
  sidebarOpen: true,
  modal: {
    isOpen: false,
    type: null,
    data: undefined,
  },
  globalLoading: false,
};

const uiSlice = createSlice({
  name: 'ui',
  initialState,
  reducers: {
    toggleSidebar: (state) => {
      state.sidebarOpen = !state.sidebarOpen;
    },
    setSidebarOpen: (state, action: PayloadAction<boolean>) => {
      state.sidebarOpen = action.payload;
    },
    openModal: (state, action: PayloadAction<{ type: string; data?: unknown }>) => {
      state.modal = {
        isOpen: true,
        type: action.payload.type,
        data: action.payload.data,
      };
    },
    closeModal: (state) => {
      state.modal = {
        isOpen: false,
        type: null,
        data: undefined,
      };
    },
    setGlobalLoading: (state, action: PayloadAction<boolean>) => {
      state.globalLoading = action.payload;
    },
  },
});

export const { toggleSidebar, setSidebarOpen, openModal, closeModal, setGlobalLoading } = uiSlice.actions;
export default uiSlice.reducer;
