import axios from 'axios';

const BASE_URL = 'http://localhost:8080';

// The single Axios instance used everywhere in the app.
// All API calls go through this — never use raw axios.create() elsewhere.
const axiosInstance = axios.create({
    baseURL: BASE_URL,
    headers: { 'Content-Type': 'application/json' },
});

// REQUEST INTERCEPTOR — injects the access token on every outgoing request.
// We read from localStorage here as a fallback for the initial load.
// AuthContext keeps the in-memory token and calls setAuthHeader() after login.
axiosInstance.interceptors.request.use(
    (config) => {
        const token = window.__accessToken;
        // ↑ We store the in-memory token on window.__accessToken so the interceptor
        //   can access it without importing AuthContext (avoids circular dependencies).
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => Promise.reject(error)
);

// RESPONSE INTERCEPTOR — handles 401 Unauthorized responses.
// If the access token expired, try to refresh it silently, then retry the
// original request. If refresh also fails, log the user out.
let isRefreshing = false;
// ↑ Flag to prevent multiple simultaneous refresh calls if several requests
//   fail with 401 at the same time (e.g., page load fires 3 API calls).

let failedQueue = [];
// ↑ Queue of requests that failed while a refresh was in progress.
//   They all retry once the new token arrives.

const processQueue = (error, token = null) => {
    failedQueue.forEach(({ resolve, reject }) => {
        if (error) reject(error);
        else resolve(token);
    });
    failedQueue = [];
};

axiosInstance.interceptors.response.use(
    (response) => response,
    async(error) => {
        const originalRequest = error.config;

        if (error.response ?.status === 401 && !originalRequest._retry) {
            // _retry flag prevents infinite retry loops if the refresh itself returns 401
            if (isRefreshing) {
                // Another refresh is already in progress — queue this request
                return new Promise((resolve, reject) => {
                    failedQueue.push({ resolve, reject });
                }).then((token) => {
                    originalRequest.headers.Authorization = `Bearer ${token}`;
                    return axiosInstance(originalRequest);
                });
            }

            originalRequest._retry = true;
            isRefreshing = true;

            const refreshToken = localStorage.getItem('refreshToken');
            if (!refreshToken) {
                // No refresh token stored — user must log in again
                window.__accessToken = null;
                processQueue(new Error('No refresh token'), null);
                isRefreshing = false;
                window.location.href = '/login';
                return Promise.reject(error);
            }

            try {
                const response = await axios.post(`${BASE_URL}/api/v1/auth/refresh`, {
                    refreshToken,
                });
                // ↑ Use raw axios here, not axiosInstance, to avoid triggering
                //   this interceptor recursively on the refresh call itself.

                const { accessToken, refreshToken: newRefreshToken } = response.data;

                window.__accessToken = accessToken;
                localStorage.setItem('refreshToken', newRefreshToken);

                processQueue(null, accessToken);
                originalRequest.headers.Authorization = `Bearer ${accessToken}`;
                return axiosInstance(originalRequest);
            } catch (refreshError) {
                processQueue(refreshError, null);
                window.__accessToken = null;
                localStorage.removeItem('refreshToken');
                window.location.href = '/login';
                return Promise.reject(refreshError);
            } finally {
                isRefreshing = false;
            }
        }

        return Promise.reject(error);
    }
);

export default axiosInstance;