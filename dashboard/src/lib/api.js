import axios from "axios";

const API_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

const api = axios.create({
  baseURL: API_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

// Add auth token to requests
api.interceptors.request.use((config) => {
  if (typeof window !== "undefined") {
    const token = localStorage.getItem("accessToken");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
  }
  return config;
});

// Handle token refresh on 401
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        const refreshToken = localStorage.getItem("refreshToken");
        if (refreshToken) {
          const response = await axios.post(`${API_URL}/api/v1/auth/refresh`, {
            refreshToken,
          });

          const { accessToken, refreshToken: newRefreshToken } =
            response.data.data;
          localStorage.setItem("accessToken", accessToken);
          localStorage.setItem("refreshToken", newRefreshToken);

          originalRequest.headers.Authorization = `Bearer ${accessToken}`;
          return api(originalRequest);
        }
      } catch (refreshError) {
        localStorage.removeItem("accessToken");
        localStorage.removeItem("refreshToken");
        window.location.href = "/login";
      }
    }

    return Promise.reject(error);
  }
);

// Auth APIs
export const authApi = {
  login: (username, password) =>
    api.post("/api/v1/auth/login", { username, password }),
  refresh: (refreshToken) => api.post("/api/v1/auth/refresh", { refreshToken }),
};

// Dashboard APIs
export const dashboardApi = {
  getSummary: (startTime, endTime) =>
    api.get("/api/v1/dashboard/summary", { params: { startTime, endTime } }),
  getTopSlowEndpoints: (limit = 5, startTime, endTime) =>
    api.get("/api/v1/dashboard/top-slow-endpoints", {
      params: { limit, startTime, endTime },
    }),
  getErrorRateGraph: (startTime, endTime, intervalMinutes = 60) =>
    api.get("/api/v1/dashboard/error-rate-graph", {
      params: { startTime, endTime, intervalMinutes },
    }),
  getLogs: (filters) => api.get("/api/v1/dashboard/logs", { params: filters }),
  getServices: () => api.get("/api/v1/dashboard/services"),
  getEndpoints: (serviceName) =>
    api.get("/api/v1/dashboard/endpoints", { params: { serviceName } }),
};

// Incident APIs
export const incidentApi = {
  getIncidents: (filters) => api.get("/api/v1/incidents", { params: filters }),
  getIncidentById: (id) => api.get(`/api/v1/incidents/${id}`),
  resolveIncident: (id, version, resolutionNotes) =>
    api.post(
      `/api/v1/incidents/${id}/resolve`,
      { resolutionNotes },
      { params: { version } }
    ),
};

// Alert APIs
export const alertApi = {
  getAlerts: (page = 0, size = 20) =>
    api.get("/api/v1/alerts", { params: { page, size } }),
  getUnacknowledgedAlerts: (page = 0, size = 20) =>
    api.get("/api/v1/alerts/unacknowledged", { params: { page, size } }),
  acknowledgeAlert: (id) => api.post(`/api/v1/alerts/${id}/acknowledge`),
};

// Health Score APIs
export const healthScoreApi = {
  getSystemHealth: (startTime, endTime) =>
    api.get("/api/v1/health-score/system", { params: { startTime, endTime } }),
  getEndpointScores: (startTime, endTime) =>
    api.get("/api/v1/health-score/endpoints", {
      params: { startTime, endTime },
    }),
  getEndpointScore: (serviceName, endpoint, startTime, endTime) =>
    api.get("/api/v1/health-score/endpoint", {
      params: { serviceName, endpoint, startTime, endTime },
    }),
  getTrend: (hours = 24, intervalMinutes = 60) =>
    api.get("/api/v1/health-score/trend", {
      params: { hours, intervalMinutes },
    }),
};

export default api;
