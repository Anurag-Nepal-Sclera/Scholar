import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import { apiClient } from '@/api/client';
import { MatchResultResponse, ApiResponse, Page } from '@/types';
import { RootState } from '../index';

interface MatchState {
  matches: MatchResultResponse[];
  pagination: {
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
  };
  filters: {
    minScore: number;
  };
  loading: boolean;
  computing: boolean;
  error: string | null;
}

const initialState: MatchState = {
  matches: [],
  pagination: {
    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0,
  },
  filters: {
    minScore: 0,
  },
  loading: false,
  computing: false,
  error: null,
};

// Async thunks
export const fetchMatches = createAsyncThunk(
  'match/fetchAll',
  async (params: { cvId: string; page?: number; size?: number }, { getState, rejectWithValue }) => {
    try {
      const state = getState() as RootState;
      const tenantId = state.tenant.currentTenant?.id;
      if (!tenantId) {
        return rejectWithValue('No tenant selected');
      }
      
      const response = await apiClient.get<ApiResponse<Page<MatchResultResponse>>>(
        `/v1/matches/cv/${params.cvId}`,
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
      return rejectWithValue(response.data.message || 'Failed to fetch matches');
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } }; message?: string };
      return rejectWithValue(err.response?.data?.message || err.message || 'Failed to fetch matches');
    }
  }
);

export const fetchMatchesAboveThreshold = createAsyncThunk(
  'match/fetchAboveThreshold',
  async (params: { cvId: string; minScore: number }, { getState, rejectWithValue }) => {
    try {
      const state = getState() as RootState;
      const tenantId = state.tenant.currentTenant?.id;
      if (!tenantId) {
        return rejectWithValue('No tenant selected');
      }
      
      const response = await apiClient.get<ApiResponse<MatchResultResponse[]>>(
        `/v1/matches/cv/${params.cvId}/above-threshold`,
        {
          params: {
            tenantId,
            minScore: params.minScore,
          },
        }
      );
      
      if (response.data.success && response.data.data) {
        return response.data.data;
      }
      return rejectWithValue(response.data.message || 'Failed to fetch matches');
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } }; message?: string };
      return rejectWithValue(err.response?.data?.message || err.message || 'Failed to fetch matches');
    }
  }
);

export const recomputeMatches = createAsyncThunk(
  'match/recompute',
  async (cvId: string, { getState, rejectWithValue }) => {
    try {
      const state = getState() as RootState;
      const tenantId = state.tenant.currentTenant?.id;
      if (!tenantId) {
        return rejectWithValue('No tenant selected');
      }
      
      await apiClient.post(`/v1/matches/cv/${cvId}/recompute`, null, {
        params: { tenantId },
      });
      
      return cvId;
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } }; message?: string };
      return rejectWithValue(err.response?.data?.message || err.message || 'Failed to recompute matches');
    }
  }
);

const matchSlice = createSlice({
  name: 'match',
  initialState,
  reducers: {
    setMinScoreFilter: (state, action: PayloadAction<number>) => {
      state.filters.minScore = action.payload;
    },
    clearMatchError: (state) => {
      state.error = null;
    },
    clearMatchState: (state) => {
      state.matches = [];
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    builder
      // Fetch matches
      .addCase(fetchMatches.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchMatches.fulfilled, (state, action) => {
        state.loading = false;
        state.matches = action.payload.content;
        state.pagination = {
          page: action.payload.number,
          size: action.payload.size,
          totalElements: action.payload.totalElements,
          totalPages: action.payload.totalPages,
        };
      })
      .addCase(fetchMatches.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      })
      // Fetch matches above threshold
      .addCase(fetchMatchesAboveThreshold.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchMatchesAboveThreshold.fulfilled, (state, action) => {
        state.loading = false;
        state.matches = action.payload;
        state.pagination = {
          ...state.pagination,
          totalElements: action.payload.length,
          totalPages: 1,
        };
      })
      .addCase(fetchMatchesAboveThreshold.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      })
      // Recompute matches
      .addCase(recomputeMatches.pending, (state) => {
        state.computing = true;
        state.error = null;
      })
      .addCase(recomputeMatches.fulfilled, (state) => {
        state.computing = false;
      })
      .addCase(recomputeMatches.rejected, (state, action) => {
        state.computing = false;
        state.error = action.payload as string;
      });
  },
});

export const { setMinScoreFilter, clearMatchError, clearMatchState } = matchSlice.actions;
export default matchSlice.reducer;
