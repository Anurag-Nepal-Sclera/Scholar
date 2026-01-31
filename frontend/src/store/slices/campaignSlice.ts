import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import { apiClient } from '@/api/client';
import { EmailCampaignResponse, EmailLogResponse, CreateCampaignRequest, ApiResponse, Page } from '@/types';
import { RootState } from '../index';

interface CampaignState {
  campaigns: EmailCampaignResponse[];
  currentCampaign: EmailCampaignResponse | null;
  logs: EmailLogResponse[];
  pagination: {
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
  };
  logsPagination: {
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
  };
  loading: boolean;
  creating: boolean;
  executing: boolean;
  logsLoading: boolean;
  error: string | null;
}

const initialState: CampaignState = {
  campaigns: [],
  currentCampaign: null,
  logs: [],
  pagination: {
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
  },
  logsPagination: {
    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0,
  },
  loading: false,
  creating: false,
  executing: false,
  logsLoading: false,
  error: null,
};

// Async thunks
export const fetchCampaigns = createAsyncThunk(
  'campaign/fetchAll',
  async (params: { page?: number; size?: number } = {}, { getState, rejectWithValue }) => {
    try {
      const state = getState() as RootState;
      const tenantId = state.tenant.currentTenant?.id;
      if (!tenantId) {
        return rejectWithValue('No tenant selected');
      }
      
      const response = await apiClient.get<ApiResponse<Page<EmailCampaignResponse>>>('/v1/campaigns', {
        params: {
          tenantId,
          page: params.page ?? 0,
          size: params.size ?? 10,
          sort: 'createdAt,desc',
        },
      });
      
      if (response.data.success && response.data.data) {
        return response.data.data;
      }
      return rejectWithValue(response.data.message || 'Failed to fetch campaigns');
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } }; message?: string };
      return rejectWithValue(err.response?.data?.message || err.message || 'Failed to fetch campaigns');
    }
  }
);

export const fetchCampaign = createAsyncThunk(
  'campaign/fetchOne',
  async (campaignId: string, { getState, rejectWithValue }) => {
    try {
      const state = getState() as RootState;
      const tenantId = state.tenant.currentTenant?.id;
      if (!tenantId) {
        return rejectWithValue('No tenant selected');
      }
      
      const response = await apiClient.get<ApiResponse<EmailCampaignResponse>>(
        `/v1/campaigns/${campaignId}`,
        { params: { tenantId } }
      );
      
      if (response.data.success && response.data.data) {
        return response.data.data;
      }
      return rejectWithValue(response.data.message || 'Failed to fetch campaign');
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } }; message?: string };
      return rejectWithValue(err.response?.data?.message || err.message || 'Failed to fetch campaign');
    }
  }
);

export const createCampaign = createAsyncThunk(
  'campaign/create',
  async (data: CreateCampaignRequest, { getState, rejectWithValue }) => {
    try {
      const state = getState() as RootState;
      const tenantId = state.tenant.currentTenant?.id;
      if (!tenantId) {
        return rejectWithValue('No tenant selected');
      }
      
      const response = await apiClient.post<ApiResponse<EmailCampaignResponse>>(
        '/v1/campaigns',
        data,
        { params: { tenantId } }
      );
      
      if (response.data.success && response.data.data) {
        return response.data.data;
      }
      return rejectWithValue(response.data.message || 'Failed to create campaign');
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } }; message?: string };
      return rejectWithValue(err.response?.data?.message || err.message || 'Failed to create campaign');
    }
  }
);

export const scheduleCampaign = createAsyncThunk(
  'campaign/schedule',
  async (params: { campaignId: string; scheduledAt: string }, { getState, rejectWithValue }) => {
    try {
      const state = getState() as RootState;
      const tenantId = state.tenant.currentTenant?.id;
      if (!tenantId) {
        return rejectWithValue('No tenant selected');
      }
      
      await apiClient.post(`/v1/campaigns/${params.campaignId}/schedule`, null, {
        params: { tenantId, scheduledAt: params.scheduledAt },
      });
      
      return params.campaignId;
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } }; message?: string };
      return rejectWithValue(err.response?.data?.message || err.message || 'Failed to schedule campaign');
    }
  }
);

export const executeCampaign = createAsyncThunk(
  'campaign/execute',
  async (campaignId: string, { getState, rejectWithValue }) => {
    try {
      const state = getState() as RootState;
      const tenantId = state.tenant.currentTenant?.id;
      if (!tenantId) {
        return rejectWithValue('No tenant selected');
      }
      
      await apiClient.post(`/v1/campaigns/${campaignId}/execute`, null, {
        params: { tenantId },
      });
      
      return campaignId;
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } }; message?: string };
      return rejectWithValue(err.response?.data?.message || err.message || 'Failed to execute campaign');
    }
  }
);

export const cancelCampaign = createAsyncThunk(
  'campaign/cancel',
  async (campaignId: string, { getState, rejectWithValue }) => {
    try {
      const state = getState() as RootState;
      const tenantId = state.tenant.currentTenant?.id;
      if (!tenantId) {
        return rejectWithValue('No tenant selected');
      }
      
      await apiClient.post(`/v1/campaigns/${campaignId}/cancel`, null, {
        params: { tenantId },
      });
      
      return campaignId;
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } }; message?: string };
      return rejectWithValue(err.response?.data?.message || err.message || 'Failed to cancel campaign');
    }
  }
);

export const fetchCampaignLogs = createAsyncThunk(
  'campaign/fetchLogs',
  async (params: { campaignId: string; page?: number; size?: number }, { getState, rejectWithValue }) => {
    try {
      const state = getState() as RootState;
      const tenantId = state.tenant.currentTenant?.id;
      if (!tenantId) {
        return rejectWithValue('No tenant selected');
      }
      
      const response = await apiClient.get<ApiResponse<Page<EmailLogResponse>>>(
        `/v1/campaigns/${params.campaignId}/logs`,
        {
          params: {
            tenantId,
            page: params.page ?? 0,
            size: params.size ?? 20,
          },
        }
      );
      
      if (response.data.success && response.data.data) {
        return response.data.data;
      }
      return rejectWithValue(response.data.message || 'Failed to fetch logs');
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } }; message?: string };
      return rejectWithValue(err.response?.data?.message || err.message || 'Failed to fetch logs');
    }
  }
);

const campaignSlice = createSlice({
  name: 'campaign',
  initialState,
  reducers: {
    setCurrentCampaign: (state, action: PayloadAction<EmailCampaignResponse | null>) => {
      state.currentCampaign = action.payload;
    },
    clearCampaignError: (state) => {
      state.error = null;
    },
    clearCampaignState: (state) => {
      state.campaigns = [];
      state.currentCampaign = null;
      state.logs = [];
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    builder
      // Fetch campaigns
      .addCase(fetchCampaigns.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchCampaigns.fulfilled, (state, action) => {
        state.loading = false;
        state.campaigns = action.payload.content;
        state.pagination = {
          page: action.payload.number,
          size: action.payload.size,
          totalElements: action.payload.totalElements,
          totalPages: action.payload.totalPages,
        };
      })
      .addCase(fetchCampaigns.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      })
      // Fetch single campaign
      .addCase(fetchCampaign.fulfilled, (state, action) => {
        state.currentCampaign = action.payload;
        // Update in list if exists
        const index = state.campaigns.findIndex((c) => c.id === action.payload.id);
        if (index !== -1) {
          state.campaigns[index] = action.payload;
        }
      })
      // Create campaign
      .addCase(createCampaign.pending, (state) => {
        state.creating = true;
        state.error = null;
      })
      .addCase(createCampaign.fulfilled, (state, action) => {
        state.creating = false;
        state.campaigns.unshift(action.payload);
        state.currentCampaign = action.payload;
      })
      .addCase(createCampaign.rejected, (state, action) => {
        state.creating = false;
        state.error = action.payload as string;
      })
      // Execute campaign
      .addCase(executeCampaign.pending, (state) => {
        state.executing = true;
      })
      .addCase(executeCampaign.fulfilled, (state) => {
        state.executing = false;
      })
      .addCase(executeCampaign.rejected, (state, action) => {
        state.executing = false;
        state.error = action.payload as string;
      })
      // Fetch logs
      .addCase(fetchCampaignLogs.pending, (state) => {
        state.logsLoading = true;
      })
      .addCase(fetchCampaignLogs.fulfilled, (state, action) => {
        state.logsLoading = false;
        state.logs = action.payload.content;
        state.logsPagination = {
          page: action.payload.number,
          size: action.payload.size,
          totalElements: action.payload.totalElements,
          totalPages: action.payload.totalPages,
        };
      })
      .addCase(fetchCampaignLogs.rejected, (state, action) => {
        state.logsLoading = false;
        state.error = action.payload as string;
      });
  },
});

export const { setCurrentCampaign, clearCampaignError, clearCampaignState } = campaignSlice.actions;
export default campaignSlice.reducer;
