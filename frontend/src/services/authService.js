import api, { ensureCsrf } from './api';

export const authService = {
  async login(email, password) {
    await ensureCsrf();
    const { data } = await api.post('/auth/login', { email, password });
    return data;
  },

  async register(payload) {
    await ensureCsrf();
    const { data } = await api.post('/auth/register', payload);
    return data;
  },

  async logout() {
    await api.post('/auth/logout');
  },

  async me() {
    const { data } = await api.get('/auth/me');
    return data;
  },

  async forgotPassword(email) {
    await ensureCsrf();
    const { data } = await api.post('/auth/forgot-password', { email });
    return data;
  },

  async resetPassword(token, newPassword, confirmPassword) {
    await ensureCsrf();
    const { data } = await api.post('/auth/reset-password', {
      token,
      newPassword,
      confirmPassword,
    });
    return data;
  },
};
