import { ShieldAlert } from 'lucide-react';

/** The fictional-bank notice required throughout the application. */
export default function Disclaimer({ compact = false }) {
  return (
    <div className={`hfdc-disclaimer ${compact ? 'hfdc-disclaimer-compact' : ''}`} role="note">
      <ShieldAlert size={18} className="flex-shrink-0" />
      <span>
        HFDC Bank is a fictional educational banking simulation. It is not a real financial
        institution.
      </span>
    </div>
  );
}
