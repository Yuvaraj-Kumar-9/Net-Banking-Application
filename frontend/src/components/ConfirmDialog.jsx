import { AlertTriangle } from 'lucide-react';

/** Small accessible confirmation modal used before destructive actions. */
export default function ConfirmDialog({ open, title, message, confirmLabel = 'Confirm', onConfirm, onCancel, busy }) {
  if (!open) return null;
  return (
    <div className="hfdc-modal-backdrop" role="presentation" onClick={onCancel}>
      <div
        className="hfdc-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="confirm-title"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="hfdc-modal-icon">
          <AlertTriangle size={24} />
        </div>
        <h3 id="confirm-title">{title}</h3>
        <p>{message}</p>
        <div className="d-flex gap-2 justify-content-end mt-4">
          <button type="button" className="btn btn-light hfdc-btn" onClick={onCancel} disabled={busy}>
            Cancel
          </button>
          <button type="button" className="btn btn-danger hfdc-btn" onClick={onConfirm} disabled={busy}>
            {busy ? 'Working…' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
