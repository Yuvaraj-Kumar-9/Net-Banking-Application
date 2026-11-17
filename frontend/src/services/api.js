import axios from 'axios';

/**
 * Single Axios instance for the whole app.
 * - withCredentials: sends the HttpOnly JWT cookie.
 * - The request interceptor copies the readable XSRF-TOKEN cookie into the
 *   X-XSRF-TOKEN header, which the backend requires on every POST/PUT/DELETE.
 */
const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
  timeout: 20000,
});

function readCookie(name) {
  const match = document.cookie
    .split('; ')
    .find((row) => row.startsWith(`${name}=`));
  return match ? decodeURIComponent(match.split('=').slice(1).join('=')) : null;
}

let csrfPromise = null;

/** Fetches (once) a CSRF cookie from the backend. */
export function ensureCsrf() {
  if (readCookie('XSRF-TOKEN')) return Promise.resolve();
  if (!csrfPromise) {
    csrfPromise = api
      .get('/auth/csrf')
      .catch(() => null)
      .finally(() => {
        csrfPromise = null;
      });
  }
  return csrfPromise;
}

api.interceptors.request.use(async (config) => {
  const method = (config.method || 'get').toUpperCase();
  if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(method)) {
    if (!readCookie('XSRF-TOKEN')) {
      await ensureCsrf();
    }
    const token = readCookie('XSRF-TOKEN');
    if (token) config.headers['X-XSRF-TOKEN'] = token;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const url = error.config?.url || '';
    if (status === 401 && !url.includes('/auth/login') && !url.includes('/auth/me')) {
      window.dispatchEvent(new Event('hfdc:unauthorized'));
    }
    return Promise.reject(error);
  },
);

/** Turns any Axios/network failure into a friendly sentence. */
export function apiErrorMessage(error) {
  if (error?.response?.data?.message) return error.response.data.message;
  if (error?.response?.status === 401) return 'Your session has expired. Please log in again.';
  if (error?.response?.status === 403) return 'You are not allowed to perform this action.';
  if (error?.response?.status >= 500) return 'The server had a problem. Please try again.';
  if (error?.code === 'ERR_NETWORK') {
    return 'Cannot reach the HFDC Bank server. Check that the backend is running.';
  }
  return error?.message || 'Something went wrong. Please try again.';
}

export default api;
