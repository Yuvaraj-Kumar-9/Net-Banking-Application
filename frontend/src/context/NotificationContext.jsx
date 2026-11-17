import { createContext, useCallback, useContext, useState } from 'react';
import Notification from '../components/Notification';

const NotificationContext = createContext(null);

/** Lightweight toast system used for success and error feedback. */
export function NotificationProvider({ children }) {
  const [toasts, setToasts] = useState([]);

  const dismiss = useCallback((id) => {
    setToasts((current) => current.filter((toast) => toast.id !== id));
  }, []);

  const notify = useCallback(
    (message, type = 'success') => {
      const id = `${Date.now()}-${Math.random()}`;
      setToasts((current) => [...current, { id, message, type }]);
      window.setTimeout(() => dismiss(id), 5500);
    },
    [dismiss],
  );

  return (
    <NotificationContext.Provider value={{ notify }}>
      {children}
      <Notification toasts={toasts} onDismiss={dismiss} />
    </NotificationContext.Provider>
  );
}

export function useNotify() {
  const context = useContext(NotificationContext);
  if (!context) throw new Error('useNotify must be used inside NotificationProvider');
  return context;
}
