import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Eye, EyeOff, LogIn, ShieldCheck, AlertCircle, ArrowRight } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useNotify } from '../context/NotificationContext';
import { apiErrorMessage } from '../services/api';
import Disclaimer from '../components/Disclaimer';
import Logo from '../components/Logo';

const DEMO = [
  { label: 'Aarav (PIN 1234)', email: 'aarav@hfdcbank.test' },
  { label: 'Priya (PIN 4321)', email: 'priya@hfdcbank.test' },
];

/** Login page: HttpOnly cookie session, no token ever touches localStorage. */
export default function Login() {
  const [form, setForm] = useState({ email: '', password: '' });
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const { login } = useAuth();
  const { notify } = useNotify();
  const navigate = useNavigate();
  const location = useLocation();
  const redirectTo = location.state?.from || '/dashboard';

  const update = (field) => (event) => {
    setForm((current) => ({ ...current, [field]: event.target.value }));
    setError('');
  };

  const submit = async (event) => {
    event.preventDefault();
    if (!form.email.trim() || !form.password) {
      setError('Please enter both your email and password.');
      return;
    }
    setBusy(true);
    setError('');
    try {
      const user = await login(form.email.trim(), form.password);
      notify(`Welcome back, ${user.fullName.split(' ')[0]}.`, 'success');
      navigate(redirectTo, { replace: true });
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const useDemo = (email) => {
    setForm({ email, password: 'Demo@1234' });
    setError('');
  };

  return (
    <section className="hfdc-auth-page">
      <div className="container">
        <div className="row g-0 hfdc-auth-shell">
          <div className="col-lg-5 hfdc-auth-side">
            <Link to="/" className="text-decoration-none">
              <Logo size={46} light />
            </Link>
            <h1>Welcome back to HFDC Bank</h1>
            <p>
              Your secure session is waiting. Sign in to see live balances, transfer funds and
              review every transaction.
            </p>
            <ul className="hfdc-auth-points">
              <li><ShieldCheck size={17} /> HttpOnly JWT cookie session</li>
              <li><ShieldCheck size={17} /> CSRF-protected form submissions</li>
              <li><ShieldCheck size={17} /> BCrypt-hashed credentials</li>
            </ul>
            <div className="hfdc-auth-demo">
              <strong>Demo credentials</strong>
              <span>aarav@hfdcbank.test / Demo@1234</span>
              <span>priya@hfdcbank.test / Demo@1234</span>
            </div>
          </div>

          <div className="col-lg-7 hfdc-auth-form-wrap">
            <div className="hfdc-auth-form">
              <h2>Sign in</h2>
              <p className="hfdc-form-sub">Use your HFDC Bank email and password.</p>

              {error && (
                <div className="alert alert-danger d-flex align-items-start gap-2" role="alert">
                  <AlertCircle size={18} className="flex-shrink-0 mt-1" />
                  <span>{error}</span>
                </div>
              )}

              <form onSubmit={submit} noValidate>
                <div className="mb-3">
                  <label className="form-label" htmlFor="login-email">Email address</label>
                  <input
                    id="login-email"
                    type="email"
                    className="form-control form-control-lg"
                    value={form.email}
                    onChange={update('email')}
                    autoComplete="email"
                    placeholder="you@example.com"
                    disabled={busy}
                  />
                </div>

                <div className="mb-3">
                  <div className="d-flex justify-content-between align-items-center">
                    <label className="form-label" htmlFor="login-password">Password</label>
                    <Link to="/forgot-password" className="hfdc-text-link small">
                      Forgot password?
                    </Link>
                  </div>
                  <div className="hfdc-input-group">
                    <input
                      id="login-password"
                      type={showPassword ? 'text' : 'password'}
                      className="form-control form-control-lg"
                      value={form.password}
                      onChange={update('password')}
                      autoComplete="current-password"
                      placeholder="Your password"
                      disabled={busy}
                    />
                    <button
                      type="button"
                      className="hfdc-input-toggle"
                      onClick={() => setShowPassword((value) => !value)}
                      aria-label={showPassword ? 'Hide password' : 'Show password'}
                    >
                      {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                    </button>
                  </div>
                </div>

                <button type="submit" className="btn btn-primary btn-lg w-100 hfdc-btn" disabled={busy}>
                  {busy ? 'Signing in…' : (<><LogIn size={18} /> Sign in securely</>)}
                </button>
              </form>

              <div className="hfdc-demo-fill">
                <span>Quick demo fill</span>
                <div className="d-flex flex-wrap gap-2">
                  {DEMO.map((demo) => (
                    <button
                      key={demo.email}
                      type="button"
                      className="btn btn-sm btn-outline-primary hfdc-chip"
                      onClick={() => useDemo(demo.email)}
                      disabled={busy}
                    >
                      {demo.label}
                    </button>
                  ))}
                </div>
              </div>

              <p className="hfdc-auth-switch">
                New to HFDC Bank? <Link to="/register">Open a demo account <ArrowRight size={14} /></Link>
              </p>

              <Disclaimer compact />
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
