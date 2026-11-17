import api from './api';

export const profileService = {
  async get() {
    const { data } = await api.get('/profile');
    return data;
  },
  async update(payload) {
    const { data } = await api.put('/profile', payload);
    return data;
  },
  async changePassword(currentPassword, newPassword, confirmPassword) {
    const { data } = await api.put('/profile/change-password', {
      currentPassword,
      newPassword,
      confirmPassword,
    });
    return data;
  },
};
