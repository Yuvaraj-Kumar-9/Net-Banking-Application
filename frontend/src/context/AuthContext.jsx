import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { authService } from '../services/authService';
import { ensureCsrf } from '../services/api';

const AuthContext = createContext(null);

/**
 * Holds the signed-in customer summary in memory only. The JWT itself lives in
 * an HttpOnly cookie, so no token is ever written to localStorage.
 */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const refresh = useCallback(async () => {
    try {
      await ensureCsrf();
      const me = await authService.me();
      setUser(me);
    } catch {
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refresh();
    const onUnauthorized = () => setUser(null);
    window.addEventListener('hfdc:unauthorized', onUnauthorized);
    return () => window.removeEventListener('hfdc:unauthorized', onUnauthorized);
  }, [refresh]);

  const login = async (email, password) => {
    const data = await authService.login(email, password);
    setUser(data);
    return data;
  };

  const register = async (payload) => authService.register(payload);

  const logout = async () => {
    try {
      await authService.logout();
    } finally {
      setUser(null);
    }
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, refresh }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside AuthProvider');
  return context;
}
