import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import { apiClient } from '@/api/client';
import { CVResponse, ApiResponse, Page } from '@/types';
import { RootState } from '../index';

interface CVState {
  cvs: CVResponse[];
  currentCV: CVResponse | null;
  parsingCVId: string | null;
  pagination: {
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
  };
  loading: boolean;
  uploading: boolean;
  error: string | null;
}

const initialState: CVState = {
  cvs: [],
  currentCV: null,
  parsingCVId: null,
  pagination: {
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
  },
  loading: false,
  uploading: false,
  error: null,
};

// Async thunks
export const fetchCVs = createAsyncThunk(
  'cv/fetchAll',
  async (params: { page?: number; size?: number } = {}, { getState, rejectWithValue }) => {
    try {
      const state = getState() as RootState;
      const tenantId = state.tenant.currentTenant?.id;
      if (!tenantId) {
        return rejectWithValue('No tenant selected');
      }

      const response = await apiClient.get<ApiResponse<Page<CVResponse>>>('/v1/cvs', {
        params: {
          tenantId,
          page: params.page ?? 0,
          size: params.size ?? 10,
          sort: 'uploadedAt,desc',
        },
      });

      if (response.data.success && response.data.data) {
        return response.data.data;
      }
      return rejectWithValue(response.data.message || 'Failed to fetch CVs');
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } }; message?: string };
      return rejectWithValue(err.response?.data?.message || err.message || 'Failed to fetch CVs');
    }
  }
);

export const fetchCV = createAsyncThunk(
  'cv/fetchOne',
  async (cvId: string, { getState, rejectWithValue }) => {
    try {
      const state = getState() as RootState;
      const tenantId = state.tenant.currentTenant?.id;
      if (!tenantId) {
        return rejectWithValue('No tenant selected');
      }

      const response = await apiClient.get<ApiResponse<CVResponse>>(`/v1/cvs/${cvId}`, {
        params: { tenantId },
      });

      if (response.data.success && response.data.data) {
        return response.data.data;
      }
      return rejectWithValue(response.data.message || 'Failed to fetch CV');
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } }; message?: string };
      return rejectWithValue(err.response?.data?.message || err.message || 'Failed to fetch CV');
    }
  }
);

export const uploadCV = createAsyncThunk(
  'cv/upload',
  async (file: File, { getState, rejectWithValue }) => {
    try {
      const state = getState() as RootState;
      const tenantId = state.tenant.currentTenant?.id;
      if (!tenantId) {
        return rejectWithValue('No tenant selected');
      }

      const formData = new FormData();
      formData.append('file', file);
      formData.append('tenantId', tenantId);

      const response = await apiClient.post<ApiResponse<CVResponse>>('/v1/cvs/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });

      if (response.data.success && response.data.data) {
        return response.data.data;
      }
      return rejectWithValue(response.data.message || 'Failed to upload CV');
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } }; message?: string };
      return rejectWithValue(err.response?.data?.message || err.message || 'Failed to upload CV');
    }
  }
);

export const parseCV = createAsyncThunk(
  'cv/parse',
  async (cvId: string, { getState, rejectWithValue }) => {
    try {
      const state = getState() as RootState;
      const tenantId = state.tenant.currentTenant?.id;
      if (!tenantId) {
        return rejectWithValue('No tenant selected');
      }

      await apiClient.post(`/v1/cvs/${cvId}/parse`, null, {
        params: { tenantId },
      });

      return cvId;
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } }; message?: string };
      return rejectWithValue(err.response?.data?.message || err.message || 'Failed to parse CV');
    }
  }
);

export const computeMatches = createAsyncThunk(
  'cv/computeMatches',
  async (cvId: string, { getState, rejectWithValue }) => {
    try {
      const state = getState() as RootState;
      const tenantId = state.tenant.currentTenant?.id;
      if (!tenantId) {
        return rejectWithValue('No tenant selected');
      }

      await apiClient.post(`/v1/cvs/${cvId}/compute-matches`, null, {
        params: { tenantId },
      });

      return cvId;
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } }; message?: string };
      return rejectWithValue(err.response?.data?.message || err.message || 'Failed to compute matches');
    }
  }
);

export const deleteCV = createAsyncThunk(
  'cv/delete',
  async (cvId: string, { getState, rejectWithValue }) => {
    try {
      const state = getState() as RootState;
      const tenantId = state.tenant.currentTenant?.id;
      if (!tenantId) {
        return rejectWithValue('No tenant selected');
      }

      await apiClient.delete(`/v1/cvs/${cvId}`, {
        params: { tenantId },
      });

      return cvId;
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } }; message?: string };
      return rejectWithValue(err.response?.data?.message || err.message || 'Failed to delete CV');
    }
  }
);

const cvSlice = createSlice({
  name: 'cv',
  initialState,
  reducers: {
    setCurrentCV: (state, action: PayloadAction<CVResponse | null>) => {
      state.currentCV = action.payload;
    },
    clearCVError: (state) => {
      state.error = null;
    },
    clearCVState: (state) => {
      state.cvs = [];
      state.currentCV = null;
      state.error = null;
    },
    updateCVInList: (state, action: PayloadAction<CVResponse>) => {
      const index = state.cvs.findIndex((cv) => cv.id === action.payload.id);
      if (index !== -1) {
        state.cvs[index] = action.payload;
      }
      if (state.currentCV?.id === action.payload.id) {
        state.currentCV = action.payload;
      }
    },
    clearParsingCVId: (state) => {
      state.parsingCVId = null;
    },
  },
  extraReducers: (builder) => {
    builder
      // Fetch CVs
      .addCase(fetchCVs.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchCVs.fulfilled, (state, action) => {
        state.loading = false;
        state.cvs = action.payload.content;
        state.pagination = {
          page: action.payload.number,
          size: action.payload.size,
          totalElements: action.payload.totalElements,
          totalPages: action.payload.totalPages,
        };
      })
      .addCase(fetchCVs.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      })
      // Fetch single CV
      .addCase(fetchCV.fulfilled, (state, action) => {
        state.currentCV = action.payload;
      })
      // Upload CV
      .addCase(uploadCV.pending, (state) => {
        state.uploading = true;
        state.error = null;
      })
      .addCase(uploadCV.fulfilled, (state, action) => {
        state.uploading = false;
        state.cvs.unshift(action.payload);
        state.parsingCVId = action.payload.id;
      })
      .addCase(uploadCV.rejected, (state, action) => {
        state.uploading = false;
        state.error = action.payload as string;
      })
      // Delete CV
      .addCase(deleteCV.fulfilled, (state, action) => {
        state.cvs = state.cvs.filter((cv) => cv.id !== action.payload);
        if (state.currentCV?.id === action.payload) {
          state.currentCV = null;
        }
      })
      // Parse CV
      .addCase(parseCV.pending, (state, action) => {
        state.parsingCVId = action.meta.arg;
      })
      .addCase(parseCV.rejected, (state) => {
        state.parsingCVId = null;
      });
  },
});

export const { setCurrentCV, clearCVError, clearCVState, updateCVInList, clearParsingCVId } = cvSlice.actions;
export default cvSlice.reducer;
