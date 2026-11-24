import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import {
  ApiResponse,
  AuthResponse,
  UserProfile,
  Address,
  AddressRequest,
  Category,
  CategoryTree,
  ListingDetail,
  ListingSummary,
  CreateListingRequest,
  UpdateListingRequest,
  GeneratorDashboardStats,
  PageResponse,
  ListingFilterParams,
  UserSummary,
  CollectorRecommendation,
  ProcessorRecommendation,
  ListingRecommendation,
  AdminOverviewStats,
  WasteCollectionAnalytics,
  RecyclingAnalytics,
  EnvironmentalImpactResponse,
  GeneratorAnalyticsResponse,
  CollectorAnalyticsResponse,
  ProcessorAnalyticsResponse,
  MonthlyTrendPoint,
  CategoryAnalyticsPoint,
  EmissionFactor,
  UpdateEmissionFactorRequest
} from '../types';

const api = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

let isRefreshing = false;
let failedQueue: Array<{
  resolve: (token: string) => void;
  reject: (err: any) => void;
}> = [];

const processQueue = (error: any, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else if (token) {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

// Request interceptor: attach Bearer token
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('accessToken');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor: handle 401 token refresh with rotation
api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    if (error.response?.status === 401 && !originalRequest._retry && !originalRequest.url?.includes('/auth/')) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then((token) => {
            if (originalRequest.headers) {
              originalRequest.headers.Authorization = `Bearer ${token}`;
            }
            return api(originalRequest);
          })
          .catch((err) => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      const refreshToken = localStorage.getItem('refreshToken');
      if (!refreshToken) {
        isRefreshing = false;
        localStorage.clear();
        window.location.href = '/login';
        return Promise.reject(error);
      }

      try {
        const { data } = await axios.post<ApiResponse<AuthResponse>>('/api/v1/auth/refresh', {
          refreshToken,
        });

        const newAccessToken = data.data.accessToken;
        const newRefreshToken = data.data.refreshToken;

        localStorage.setItem('accessToken', newAccessToken);
        localStorage.setItem('refreshToken', newRefreshToken);

        if (originalRequest.headers) {
          originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        }
        processQueue(null, newAccessToken);
        return api(originalRequest);
      } catch (refreshErr) {
        processQueue(refreshErr, null);
        localStorage.clear();
        window.location.href = '/login';
        return Promise.reject(refreshErr);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

export default api;

// API Service Functions
export const authApi = {
  login: async (credentials: { email: string; password: string }) => {
    const res = await api.post<ApiResponse<AuthResponse>>('/auth/login', credentials);
    return res.data;
  },
  register: async (userData: any) => {
    const res = await api.post<ApiResponse<AuthResponse>>('/auth/register', userData);
    return res.data;
  },
  logout: async () => {
    const refreshToken = localStorage.getItem('refreshToken');
    try {
      await api.post('/auth/logout', { refreshToken });
    } finally {
      localStorage.clear();
    }
  },
};

export const userApi = {
  getProfile: async () => {
    const res = await api.get<ApiResponse<UserProfile>>('/users/me');
    return res.data.data;
  },
  updateProfile: async (data: { firstName: string; lastName: string; phone?: string }) => {
    const res = await api.put<ApiResponse<UserProfile>>('/users/me', data);
    return res.data.data;
  },
  getAllUsers: async () => {
    const res = await api.get<ApiResponse<UserSummary[]>>('/admin/users');
    return res.data.data;
  }
};

export const addressApi = {
  getMyAddresses: async () => {
    const res = await api.get<ApiResponse<Address[]>>('/addresses');
    return res.data.data;
  },
  getAddress: async (id: number) => {
    const res = await api.get<ApiResponse<Address>>(`/addresses/${id}`);
    return res.data.data;
  },
  createAddress: async (data: AddressRequest) => {
    const res = await api.post<ApiResponse<Address>>('/addresses', data);
    return res.data.data;
  },
  updateAddress: async (id: number, data: AddressRequest) => {
    const res = await api.put<ApiResponse<Address>>(`/addresses/${id}`, data);
    return res.data.data;
  },
  deleteAddress: async (id: number) => {
    const res = await api.delete<ApiResponse<void>>(`/addresses/${id}`);
    return res.data;
  },
  setDefault: async (id: number) => {
    const res = await api.patch<ApiResponse<Address>>(`/addresses/${id}/default`);
    return res.data.data;
  },
};

export const categoryApi = {
  getTree: async (activeOnly = true) => {
    const res = await api.get<ApiResponse<CategoryTree[]>>(`/categories?hierarchical=true&activeOnly=${activeOnly}`);
    return res.data.data;
  },
  getFlat: async (activeOnly = true) => {
    const res = await api.get<ApiResponse<Category[]>>(`/categories?hierarchical=false&activeOnly=${activeOnly}`);
    return res.data.data;
  },
  getCategory: async (id: number) => {
    const res = await api.get<ApiResponse<Category>>(`/categories/${id}`);
    return res.data.data;
  },
  createCategory: async (data: any) => {
    const res = await api.post<ApiResponse<Category>>('/categories', data);
    return res.data.data;
  },
  updateCategory: async (id: number, data: any) => {
    const res = await api.put<ApiResponse<Category>>(`/categories/${id}`, data);
    return res.data.data;
  },
  setStatus: async (id: number, active: boolean) => {
    const res = await api.patch<ApiResponse<Category>>(`/categories/${id}/status`, { active });
    return res.data.data;
  },
};

export const listingApi = {
  search: async (params: ListingFilterParams) => {
    const res = await api.get<ApiResponse<PageResponse<ListingSummary>>>('/listings', { params });
    return res.data.data;
  },
  getById: async (id: number) => {
    const res = await api.get<ApiResponse<ListingDetail>>(`/listings/${id}`);
    return res.data.data;
  },
  create: async (data: CreateListingRequest) => {
    const res = await api.post<ApiResponse<ListingDetail>>('/listings', data);
    return res.data.data;
  },
  update: async (id: number, data: UpdateListingRequest) => {
    const res = await api.put<ApiResponse<ListingDetail>>(`/listings/${id}`, data);
    return res.data.data;
  },
  publish: async (id: number) => {
    const res = await api.patch<ApiResponse<ListingDetail>>(`/listings/${id}/publish`);
    return res.data.data;
  },
  cancel: async (id: number) => {
    const res = await api.patch<ApiResponse<ListingDetail>>(`/listings/${id}/cancel`);
    return res.data.data;
  },
  getMyListings: async (status?: string, page = 0, size = 10) => {
    const res = await api.get<ApiResponse<PageResponse<ListingSummary>>>('/listings/my', {
      params: { status: status || undefined, page, size }
    });
    return res.data.data;
  },
  uploadImage: async (id: number, file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    const res = await api.post(`/listings/${id}/images`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    return res.data.data;
  },
  deleteImage: async (listingId: number, imageId: number) => {
    const res = await api.delete(`/listings/${listingId}/images/${imageId}`);
    return res.data;
  }
};

export const dashboardApi = {
  getGeneratorStats: async () => {
    const res = await api.get<ApiResponse<GeneratorDashboardStats>>('/dashboard/generator');
    return res.data.data;
  },
};

export const matchingApi = {
  getRecommendedCollectors: async (listingId: number) => {
    const res = await api.get<ApiResponse<CollectorRecommendation[]>>(`/matching/listings/${listingId}/collectors`);
    return res.data.data;
  },
  getRecommendedProcessors: async (listingId: number) => {
    const res = await api.get<ApiResponse<ProcessorRecommendation[]>>(`/matching/listings/${listingId}/processors`);
    return res.data.data;
  },
  getCollectorRecommendedListings: async () => {
    const res = await api.get<ApiResponse<ListingRecommendation[]>>('/matching/collectors/me/recommended-listings');
    return res.data.data;
  },
  getProcessorRecommendedListings: async () => {
    const res = await api.get<ApiResponse<ListingRecommendation[]>>('/matching/processors/me/recommended-listings');
    return res.data.data;
  },
};

export const analyticsApi = {
  getAdminOverview: async (startDate?: string, endDate?: string) => {
    const res = await api.get<ApiResponse<AdminOverviewStats>>('/analytics/admin/overview', {
      params: { startDate: startDate || undefined, endDate: endDate || undefined }
    });
    return res.data.data;
  },
  getWasteCollectionAnalytics: async (startDate?: string, endDate?: string, categoryId?: number) => {
    const res = await api.get<ApiResponse<WasteCollectionAnalytics>>('/analytics/admin/waste', {
      params: { startDate: startDate || undefined, endDate: endDate || undefined, categoryId: categoryId || undefined }
    });
    return res.data.data;
  },
  getRecyclingAnalytics: async (startDate?: string, endDate?: string, categoryId?: number) => {
    const res = await api.get<ApiResponse<RecyclingAnalytics>>('/analytics/admin/recycling', {
      params: { startDate: startDate || undefined, endDate: endDate || undefined, categoryId: categoryId || undefined }
    });
    return res.data.data;
  },
  getEnvironmentalImpact: async (startDate?: string, endDate?: string, categoryId?: number) => {
    const res = await api.get<ApiResponse<EnvironmentalImpactResponse>>('/analytics/admin/environmental-impact', {
      params: { startDate: startDate || undefined, endDate: endDate || undefined, categoryId: categoryId || undefined }
    });
    return res.data.data;
  },
  getGeneratorAnalytics: async (startDate?: string, endDate?: string) => {
    const res = await api.get<ApiResponse<GeneratorAnalyticsResponse>>('/analytics/generator/me', {
      params: { startDate: startDate || undefined, endDate: endDate || undefined }
    });
    return res.data.data;
  },
  getCollectorAnalytics: async (startDate?: string, endDate?: string) => {
    const res = await api.get<ApiResponse<CollectorAnalyticsResponse>>('/analytics/collector/me', {
      params: { startDate: startDate || undefined, endDate: endDate || undefined }
    });
    return res.data.data;
  },
  getProcessorAnalytics: async (startDate?: string, endDate?: string) => {
    const res = await api.get<ApiResponse<ProcessorAnalyticsResponse>>('/analytics/processor/me', {
      params: { startDate: startDate || undefined, endDate: endDate || undefined }
    });
    return res.data.data;
  },
  getMonthlyTrends: async (startDate?: string, endDate?: string) => {
    const res = await api.get<ApiResponse<MonthlyTrendPoint[]>>('/analytics/trends', {
      params: { startDate: startDate || undefined, endDate: endDate || undefined }
    });
    return res.data.data;
  },
  getCategoryAnalytics: async (startDate?: string, endDate?: string) => {
    const res = await api.get<ApiResponse<CategoryAnalyticsPoint[]>>('/analytics/categories', {
      params: { startDate: startDate || undefined, endDate: endDate || undefined }
    });
    return res.data.data;
  },
  getEmissionFactors: async () => {
    const res = await api.get<ApiResponse<EmissionFactor[]>>('/analytics/factors');
    return res.data.data;
  },
  updateEmissionFactor: async (id: number, data: UpdateEmissionFactorRequest) => {
    const res = await api.put<ApiResponse<EmissionFactor>>(`/analytics/admin/factors/${id}`, data);
    return res.data.data;
  }
};
