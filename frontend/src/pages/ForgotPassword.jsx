import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AlertCircle, ArrowLeft, ArrowRight, KeyRound, MailCheck, ShieldCheck } from 'lucide-react';
import { authService } from '../services/authService';
import { apiErrorMessage } from '../services/api';
import { useNotify } from '../context/NotificationContext';
import { passwordStrength } from '../utils/format';
import Disclaimer from '../components/Disclaimer';
import Logo from '../components/Logo';

/**
 * Two-step password reset.
 * Step 1 asks for the email and (in DEV_MODE) receives a reset token on screen.
 * Step 2 exchanges that token for a new password.
 */
export default function ForgotPassword() {
  const [step, setStep] = useState(1);
  const [email, setEmail] = useState('');
  const [token, setToken] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const { notify } = useNotify();
  const navigate = useNavigate();
  const strength = passwordStrength(newPassword);

  const requestToken = async (event) => {
    event.preventDefault();
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
      setError('Enter the email address registered with HFDC Bank.');
      return;
    }
    setBusy(true);
    setError('');
    try {
      const data = await authService.forgotPassword(email.trim());
      setMessage(data.message);
      if (data.resetToken) {
        setToken(data.resetToken);
        setStep(2);
        notify('Demo reset token generated below.', 'info');
      }
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const resetPassword = async (event) => {
    event.preventDefault();
    if (!token.trim()) {
      setError('Paste the reset token you received.');
      return;
    }
    if (newPassword !== confirmPassword) {
      setError('New password and confirmation do not match.');
      return;
    }
    if (strength.score < 4) {
      setError('Choose a stronger password: 8+ characters with upper, lower, digit and symbol.');
      return;
    }
    setBusy(true);
    setError('');
    try {
      await authService.resetPassword(token.trim(), newPassword, confirmPassword);
      notify('Password reset. Please sign in with your new password.', 'success');
      navigate('/login', { replace: true });
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="hfdc-auth-page">
      <div className="container">
        <div className="row g-0 hfdc-auth-shell">
          <div className="col-lg-5 hfdc-auth-side">
            <Link to="/" className="text-decoration-none">
              <Logo size={46} light />
            </Link>
            <h1>Reset your password</h1>
            <p>
              We issue a short-lived, single-use token. In this educational build there is no email
              server, so the token is shown on screen in demo mode.
            </p>
            <ul className="hfdc-auth-points">
              <li><ShieldCheck size={17} /> Tokens expire after 30 minutes</li>
              <li><ShieldCheck size={17} /> Only a SHA-256 hash is stored</li>
              <li><ShieldCheck size={17} /> Each token works exactly once</li>
            </ul>
          </div>

          <div className="col-lg-7 hfdc-auth-form-wrap">
            <div className="hfdc-auth-form">
              <div className="hfdc-step-badge">
                {step === 1 ? <MailCheck size={18} /> : <KeyRound size={18} />}
                Step {step} of 2
              </div>
              <h2>{step === 1 ? 'Find your account' : 'Choose a new password'}</h2>

              {error && (
                <div className="alert alert-danger d-flex align-items-start gap-2" role="alert">
                  <AlertCircle size={18} className="flex-shrink-0 mt-1" />
                  <span>{error}</span>
                </div>
              )}

              {message && step === 1 && (
                <div className="alert alert-info" role="status">{message}</div>
              )}

              {step === 1 ? (
                <form onSubmit={requestToken} noValidate>
                  <div className="mb-3">
                    <label className="form-label" htmlFor="forgot-email">Registered email</label>
                    <input
                      id="forgot-email"
                      type="email"
                      className="form-control form-control-lg"
                      value={email}
                      onChange={(event) => {
                        setEmail(event.target.value);
                        setError('');
                      }}
                      placeholder="you@example.com"
                      autoComplete="email"
                      disabled={busy}
                    />
                  </div>
                  <button type="submit" className="btn btn-primary btn-lg w-100 hfdc-btn" disabled={busy}>
                    {busy ? 'Checking…' : 'Generate reset token'}
                  </button>
                  <p className="hfdc-form-note">
                    For privacy, the response is identical for registered and unknown emails.
                  </p>
                </form>
              ) : (
                <form onSubmit={resetPassword} noValidate>
                  {message && <div className="alert alert-info" role="status">{message}</div>}

                  <div className="mb-3">
                    <label className="form-label" htmlFor="reset-token">Reset token</label>
                    <textarea
                      id="reset-token"
                      rows={3}
                      className="form-control hfdc-token-box"
                      value={token}
                      onChange={(event) => setToken(event.target.value)}
                    />
                    <div className="form-text">Pre-filled in demo mode. Paste a real token here in production.</div>
                  </div>

                  <div className="mb-3">
                    <label className="form-label" htmlFor="reset-password">New password</label>
                    <input
                      id="reset-password"
                      type="password"
                      className="form-control"
                      value={newPassword}
                      onChange={(event) => setNewPassword(event.target.value)}
                      autoComplete="new-password"
                    />
                    {newPassword && (
                      <div className={`hfdc-strength hfdc-strength-${strength.tone}`}>
                        <div className="hfdc-strength-bar">
                          <span style={{ width: `${(strength.score / 5) * 100}%` }} />
                        </div>
                        <small>{strength.label}</small>
                      </div>
                    )}
                  </div>

                  <div className="mb-3">
                    <label className="form-label" htmlFor="reset-confirm">Confirm new password</label>
                    <input
                      id="reset-confirm"
                      type="password"
                      className="form-control"
                      value={confirmPassword}
                      onChange={(event) => setConfirmPassword(event.target.value)}
                      autoComplete="new-password"
                    />
                  </div>

                  <button type="submit" className="btn btn-primary btn-lg w-100 hfdc-btn" disabled={busy}>
                    {busy ? 'Updating…' : 'Set new password'}
                  </button>
                </form>
              )}

              <div className="d-flex justify-content-between align-items-center mt-4">
                <Link to="/login" className="hfdc-text-link small">
                  <ArrowLeft size={14} /> Back to sign in
                </Link>
                {step === 2 && (
                  <button
                    type="button"
                    className="btn btn-link btn-sm p-0 hfdc-text-link"
                    onClick={() => {
                      setStep(1);
                      setError('');
                    }}
                  >
                    Use a different email <ArrowRight size={14} />
                  </button>
                )}
              </div>

              <Disclaimer compact />
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
