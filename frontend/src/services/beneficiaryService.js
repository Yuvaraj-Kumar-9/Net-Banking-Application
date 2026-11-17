import api from './api';

export const beneficiaryService = {
  async list(search = '') {
    const { data } = await api.get('/beneficiaries', { params: search ? { search } : {} });
    return data;
  },
  async add(name, accountNumber) {
    const { data } = await api.post('/beneficiaries', { name, accountNumber });
    return data;
  },
  async remove(id) {
    const { data } = await api.delete(`/beneficiaries/${id}`);
    return data;
  },
};
