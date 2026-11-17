import api from './api';

export const transferService = {
  async transfer(payload) {
    const { data } = await api.post('/transfers', payload);
    return data;
  },
  async history() {
    const { data } = await api.get('/transfers/history');
    return data;
  },
};
