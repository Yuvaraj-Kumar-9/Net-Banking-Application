import { CheckCircle2, AlertCircle, Info, X } from 'lucide-react';

const ICONS = {
  success: CheckCircle2,
  danger: AlertCircle,
  error: AlertCircle,
  warning: AlertCircle,
  info: Info,
};

/** Toast stack rendered in the bottom-right corner. */
export default function Notification({ toasts, onDismiss }) {
  if (!toasts.length) return null;
  return (
    <div className="hfdc-toast-stack" aria-live="polite" aria-atomic="true">
      {toasts.map((toast) => {
        const Icon = ICONS[toast.type] || Info;
        return (
          <div key={toast.id} className={`hfdc-toast hfdc-toast-${toast.type}`} role="status">
            <Icon size={20} className="flex-shrink-0" />
            <span className="flex-grow-1">{toast.message}</span>
            <button
              type="button"
              className="btn btn-sm btn-link text-white p-0"
              onClick={() => onDismiss(toast.id)}
              aria-label="Dismiss notification"
            >
              <X size={16} />
            </button>
          </div>
        );
      })}
    </div>
  );
}
