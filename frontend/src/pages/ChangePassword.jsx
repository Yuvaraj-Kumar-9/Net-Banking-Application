import { useState } from 'react';
import { Link } from 'react-router-dom';
import {
  AlertCircle,
  CheckCircle2,
  Eye,
  EyeOff,
  KeyRound,
  ShieldCheck,
  Lock,
} from 'lucide-react';
import { profileService } from '../services/profileService';
import { apiErrorMessage } from '../services/api';
import { passwordStrength } from '../utils/format';
import { useNotify } from '../context/NotificationContext';
import PageHeader from '../components/PageHeader';

/** Authenticated password change with strength feedback. */
export default function ChangePassword() {
  const [form, setForm] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' });
  const [show, setShow] = useState({ current: false, next: false });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [busy, setBusy] = useState(false);
  const { notify } = useNotify();
  const strength = passwordStrength(form.newPassword);

  const update = (field) => (event) => {
    setForm((current) => ({ ...current, [field]: event.target.value }));
    setError('');
    setSuccess(false);
  };

  const submit = async (event) => {
    event.preventDefault();
    if (!form.currentPassword) {
      setError('Enter your current password.');
      return;
    }
    if (strength.score < 4) {
      setError('Choose a stronger new password: 8+ characters with upper, lower, digit and symbol.');
      return;
    }
    if (form.newPassword !== form.confirmPassword) {
      setError('New password and confirmation do not match.');
      return;
    }
    if (form.newPassword === form.currentPassword) {
      setError('The new password must be different from the current one.');
      return;
    }

    setBusy(true);
    setError('');
    try {
      await profileService.changePassword(
        form.currentPassword,
        form.newPassword,
        form.confirmPassword,
      );
      setSuccess(true);
      setForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
      notify('Password changed successfully.', 'success');
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <PageHeader
        title="Change password"
        subtitle="Update your login credentials. Your transaction PIN stays unchanged."
      />

      <div className="row g-4">
        <div className="col-lg-7">
          <div className="hfdc-form-card">
            <h2 className="hfdc-form-title">
              <KeyRound size={20} /> Security credentials
            </h2>

            {error && (
              <div className="alert alert-danger d-flex align-items-start gap-2" role="alert">
                <AlertCircle size={18} className="flex-shrink-0 mt-1" />
                <span>{error}</span>
              </div>
            )}

            {success && (
              <div className="alert alert-success d-flex align-items-center gap-2" role="status">
                <CheckCircle2 size={18} />
                <span>Your password has been updated. Use it the next time you sign in.</span>
              </div>
            )}

            <form onSubmit={submit} noValidate>
              <div className="mb-3">
                <label className="form-label" htmlFor="current-password">Current password</label>
                <div className="hfdc-input-group">
                  <input
                    id="current-password"
                    type={show.current ? 'text' : 'password'}
                    className="form-control"
                    value={form.currentPassword}
                    onChange={update('currentPassword')}
                    autoComplete="current-password"
                    disabled={busy}
                  />
                  <button
                    type="button"
                    className="hfdc-input-toggle"
                    onClick={() => setShow((current) => ({ ...current, current: !current.current }))}
                    aria-label={show.current ? 'Hide current password' : 'Show current password'}
                  >
                    {show.current ? <EyeOff size={18} /> : <Eye size={18} />}
                  </button>
                </div>
              </div>

              <div className="mb-3">
                <label className="form-label" htmlFor="new-password">New password</label>
                <div className="hfdc-input-group">
                  <input
                    id="new-password"
                    type={show.next ? 'text' : 'password'}
                    className="form-control"
                    value={form.newPassword}
                    onChange={update('newPassword')}
                    autoComplete="new-password"
                    disabled={busy}
                  />
                  <button
                    type="button"
                    className="hfdc-input-toggle"
                    onClick={() => setShow((current) => ({ ...current, next: !current.next }))}
                    aria-label={show.next ? 'Hide new password' : 'Show new password'}
                  >
                    {show.next ? <EyeOff size={18} /> : <Eye size={18} />}
                  </button>
                </div>
                {form.newPassword && (
                  <div className={`hfdc-strength hfdc-strength-${strength.tone}`}>
                    <div className="hfdc-strength-bar">
                      <span style={{ width: `${(strength.score / 5) * 100}%` }} />
                    </div>
                    <small>{strength.label}</small>
                  </div>
                )}
              </div>

              <div className="mb-4">
                <label className="form-label" htmlFor="confirm-new-password">Confirm new password</label>
                <input
                  id="confirm-new-password"
                  type={show.next ? 'text' : 'password'}
                  className="form-control"
                  value={form.confirmPassword}
                  onChange={update('confirmPassword')}
                  autoComplete="new-password"
                  disabled={busy}
                />
              </div>

              <div className="d-flex flex-wrap gap-2">
                <button type="submit" className="btn btn-primary hfdc-btn" disabled={busy}>
                  {busy ? 'Updating…' : (<><Lock size={16} /> Update password</>)}
                </button>
                <Link to="/profile" className="btn btn-light hfdc-btn">
                  Back to profile
                </Link>
              </div>
            </form>
          </div>
        </div>

        <div className="col-lg-5">
          <div className="hfdc-panel">
            <h3 className="hfdc-panel-title">
              <ShieldCheck size={18} /> Password rules
            </h3>
            <ul className="hfdc-check-list">
              <li>At least 8 characters long.</li>
              <li>Contains upper case and lower case letters.</li>
              <li>Contains at least one digit.</li>
              <li>Contains one of @ # $ % ^ &amp; + = !</li>
              <li>Must differ from your current password.</li>
            </ul>
            <div className="hfdc-security-note">
              <KeyRound size={18} />
              <p>
                Passwords are hashed with BCrypt(12) before storage. The server never logs request
                bodies, so plain-text passwords never reach a log file.
              </p>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
