import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import { apiClient } from '@/api/client';
import { TenantResponse, TenantDashboardResponse, TenantRequest, ApiResponse } from '@/types';

interface TenantState {
  tenants: TenantResponse[];
  currentTenant: TenantResponse | null;
  dashboard: TenantDashboardResponse | null;
  loading: boolean;
  dashboardLoading: boolean;
  error: string | null;
}

const initialState: TenantState = {
  tenants: [],
  currentTenant: null,
  dashboard: null,
  loading: false,
  dashboardLoading: false,
  error: null,
};

// Async thunks
export const fetchTenants = createAsyncThunk(
  'tenant/fetchAll',
  async (_, { rejectWithValue }) => {
    try {
      const response = await apiClient.get<ApiResponse<TenantResponse[]>>('/v1/tenants');
      if (response.data.success && response.data.data) {
        return response.data.data;
      }
      return rejectWithValue(response.data.message || 'Failed to fetch tenants');
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } }; message?: string };
      return rejectWithValue(err.response?.data?.message || err.message || 'Failed to fetch tenants');
    }
  }
);

export const createTenant = createAsyncThunk(
  'tenant/create',
  async (data: TenantRequest, { rejectWithValue }) => {
    try {
      const response = await apiClient.post<ApiResponse<TenantResponse>>('/v1/tenants', data);
      if (response.data.success && response.data.data) {
        return response.data.data;
      }
      return rejectWithValue(response.data.message || 'Failed to create tenant');
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } }; message?: string };
      return rejectWithValue(err.response?.data?.message || err.message || 'Failed to create tenant');
    }
  }
);

export const deleteTenant = createAsyncThunk(
  'tenant/delete',
  async (tenantId: string, { rejectWithValue }) => {
    try {
      await apiClient.delete(`/v1/tenants/${tenantId}`);
      return tenantId;
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } }; message?: string };
      return rejectWithValue(err.response?.data?.message || err.message || 'Failed to delete tenant');
    }
  }
);

export const fetchDashboard = createAsyncThunk(
  'tenant/fetchDashboard',
  async (tenantId: string, { rejectWithValue }) => {
    try {
      const response = await apiClient.get<ApiResponse<TenantDashboardResponse>>(
        `/v1/tenants/${tenantId}/stats`
      );
      if (response.data.success && response.data.data) {
        return response.data.data;
      }
      return rejectWithValue(response.data.message || 'Failed to fetch dashboard');
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } }; message?: string };
      return rejectWithValue(err.response?.data?.message || err.message || 'Failed to fetch dashboard');
    }
  }
);

const tenantSlice = createSlice({
  name: 'tenant',
  initialState,
  reducers: {
    setCurrentTenant: (state, action: PayloadAction<TenantResponse | null>) => {
      state.currentTenant = action.payload;
      state.dashboard = null; // Reset dashboard when tenant changes
    },
    clearTenantError: (state) => {
      state.error = null;
    },
    clearTenantState: (state) => {
      state.tenants = [];
      state.currentTenant = null;
      state.dashboard = null;
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    builder
      // Fetch tenants
      .addCase(fetchTenants.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchTenants.fulfilled, (state, action) => {
        state.loading = false;
        state.tenants = action.payload;
        // Auto-select first tenant if none selected
        if (!state.currentTenant && action.payload.length > 0) {
          state.currentTenant = action.payload[0];
        }
      })
      .addCase(fetchTenants.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      })
      // Create tenant
      .addCase(createTenant.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(createTenant.fulfilled, (state, action) => {
        state.loading = false;
        state.tenants.push(action.payload);
        state.currentTenant = action.payload;
      })
      .addCase(createTenant.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      })
      // Delete tenant
      .addCase(deleteTenant.fulfilled, (state, action) => {
        state.tenants = state.tenants.filter((t) => t.id !== action.payload);
        if (state.currentTenant?.id === action.payload) {
          state.currentTenant = state.tenants[0] || null;
        }
      })
      // Fetch dashboard
      .addCase(fetchDashboard.pending, (state) => {
        state.dashboardLoading = true;
      })
      .addCase(fetchDashboard.fulfilled, (state, action) => {
        state.dashboardLoading = false;
        state.dashboard = action.payload;
      })
      .addCase(fetchDashboard.rejected, (state, action) => {
        state.dashboardLoading = false;
        state.error = action.payload as string;
      });
  },
});

export const { setCurrentTenant, clearTenantError, clearTenantState } = tenantSlice.actions;
export default tenantSlice.reducer;
