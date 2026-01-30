import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { apiClient } from '@/api/client';
import { SmtpAccountResponse, SmtpAccountRequest, ApiResponse } from '@/types';
import { RootState } from '../index';

interface SmtpState {
  account: SmtpAccountResponse | null;
  loading: boolean;
  saving: boolean;
  error: string | null;
}

const initialState: SmtpState = {
  account: null,
  loading: false,
  saving: false,
  error: null,
};

// Async thunks
export const fetchSmtpAccount = createAsyncThunk(
  'smtp/fetch',
  async (_, { getState, rejectWithValue }) => {
    try {
      const state = getState() as RootState;
      const tenantId = state.tenant.currentTenant?.id;
      if (!tenantId) {
        return rejectWithValue('No tenant selected');
      }
      
      const response = await apiClient.get<ApiResponse<SmtpAccountResponse>>('/v1/smtp', {
        params: { tenantId },
      });
      
      if (response.data.success && response.data.data) {
        return response.data.data;
      }
      return rejectWithValue(response.data.message || 'Failed to fetch SMTP account');
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string }; status?: number }; message?: string };
      // 404 means no SMTP configured yet, which is valid
      if (err.response?.status === 404) {
        return null;
      }
      return rejectWithValue(err.response?.data?.message || err.message || 'Failed to fetch SMTP account');
    }
  }
);

export const saveSmtpAccount = createAsyncThunk(
  'smtp/save',
  async (data: SmtpAccountRequest, { getState, rejectWithValue }) => {
    try {
      const state = getState() as RootState;
      const tenantId = state.tenant.currentTenant?.id;
      if (!tenantId) {
        return rejectWithValue('No tenant selected');
      }
      
      await apiClient.post('/v1/smtp', data, {
        params: { tenantId },
      });
      
      // Fetch the updated account
      const response = await apiClient.get<ApiResponse<SmtpAccountResponse>>('/v1/smtp', {
        params: { tenantId },
      });
      
      if (response.data.success && response.data.data) {
        return response.data.data;
      }
      return rejectWithValue('Failed to fetch updated SMTP account');
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } }; message?: string };
      return rejectWithValue(err.response?.data?.message || err.message || 'Failed to save SMTP account');
    }
  }
);

export const deactivateSmtpAccount = createAsyncThunk(
  'smtp/deactivate',
  async (_, { getState, rejectWithValue }) => {
    try {
      const state = getState() as RootState;
      const tenantId = state.tenant.currentTenant?.id;
      if (!tenantId) {
        return rejectWithValue('No tenant selected');
      }
      
      await apiClient.post('/v1/smtp/deactivate', null, {
        params: { tenantId },
      });
      
      return true;
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } }; message?: string };
      return rejectWithValue(err.response?.data?.message || err.message || 'Failed to deactivate SMTP account');
    }
  }
);

const smtpSlice = createSlice({
  name: 'smtp',
  initialState,
  reducers: {
    clearSmtpError: (state) => {
      state.error = null;
    },
    clearSmtpState: (state) => {
      state.account = null;
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    builder
      // Fetch
      .addCase(fetchSmtpAccount.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchSmtpAccount.fulfilled, (state, action) => {
        state.loading = false;
        state.account = action.payload;
      })
      .addCase(fetchSmtpAccount.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      })
      // Save
      .addCase(saveSmtpAccount.pending, (state) => {
        state.saving = true;
        state.error = null;
      })
      .addCase(saveSmtpAccount.fulfilled, (state, action) => {
        state.saving = false;
        state.account = action.payload;
      })
      .addCase(saveSmtpAccount.rejected, (state, action) => {
        state.saving = false;
        state.error = action.payload as string;
      })
      // Deactivate
      .addCase(deactivateSmtpAccount.fulfilled, (state) => {
        if (state.account) {
          state.account.status = 'INACTIVE';
        }
      });
  },
});

export const { clearSmtpError, clearSmtpState } = smtpSlice.actions;
export default smtpSlice.reducer;
