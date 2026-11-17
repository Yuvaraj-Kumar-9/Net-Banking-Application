import api from './api';

export const transactionService = {
  async deposit(amount, description) {
    const { data } = await api.post('/transactions/deposit', { amount, description });
    return data;
  },

  async withdraw(amount, transactionPin, description) {
    const { data } = await api.post('/transactions/withdraw', {
      amount,
      transactionPin,
      description,
    });
    return data;
  },

  async list(params = {}) {
    const { data } = await api.get('/transactions', { params });
    return data;
  },

  async detail(referenceNumber) {
    const { data } = await api.get(`/transactions/${encodeURIComponent(referenceNumber)}`);
    return data;
  },
};
