import api from './api';

export const accountService = {
  async myAccount() {
    const { data } = await api.get('/accounts/me');
    return data;
  },
  async balance() {
    const { data } = await api.get('/accounts/balance');
    return data;
  },
  async dashboard() {
    const { data } = await api.get('/accounts/dashboard');
    return data;
  },
};
