import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  AlertCircle,
  ArrowRight,
  CheckCircle2,
  Eye,
  EyeOff,
  ShieldCheck,
  UserPlus,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useNotify } from '../context/NotificationContext';
import { apiErrorMessage } from '../services/api';
import { passwordStrength } from '../utils/format';
import Disclaimer from '../components/Disclaimer';
import Logo from '../components/Logo';

const INITIAL = {
  fullName: '',
  email: '',
  mobileNumber: '',
  dateOfBirth: '',
  address: '',
  password: '',
  confirmPassword: '',
  transactionPin: '',
};

const STRONG_PASSWORD = /^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$/;

/** Registration form. Mirrors the server-side Bean Validation rules. */
export default function Register() {
  const [form, setForm] = useState(INITIAL);
  const [errors, setErrors] = useState({});
  const [serverError, setServerError] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [busy, setBusy] = useState(false);
  const { register } = useAuth();
  const { notify } = useNotify();
  const navigate = useNavigate();
  const strength = passwordStrength(form.password);

  const update = (field) => (event) => {
    setForm((current) => ({ ...current, [field]: event.target.value }));
    setErrors((current) => ({ ...current, [field]: undefined }));
    setServerError('');
  };

  const validate = () => {
    const next = {};
    if (!form.fullName.trim()) next.fullName = 'Full name is required';
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
      next.email = 'Enter a valid email address';
    }
    if (!/^[0-9]{10}$/.test(form.mobileNumber.trim())) {
      next.mobileNumber = 'Mobile number must be exactly 10 digits';
    }
    if (!form.dateOfBirth) {
      next.dateOfBirth = 'Date of birth is required';
    } else {
      const dob = new Date(form.dateOfBirth);
      const age = (Date.now() - dob.getTime()) / (365.25 * 24 * 60 * 60 * 1000);
      if (Number.isNaN(dob.getTime()) || age < 18) next.dateOfBirth = 'You must be at least 18 years old';
    }
    if (!form.address.trim()) next.address = 'Address is required';
    if (!STRONG_PASSWORD.test(form.password)) {
      next.password = 'Use 8+ characters with upper, lower, digit and special character';
    }
    if (form.confirmPassword !== form.password) {
      next.confirmPassword = 'Passwords do not match';
    }
    if (!/^[0-9]{4,6}$/.test(form.transactionPin)) {
      next.transactionPin = 'Transaction PIN must be 4 to 6 digits';
    }
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const submit = async (event) => {
    event.preventDefault();
    if (!validate()) return;
    setBusy(true);
    setServerError('');
    try {
      await register({
        fullName: form.fullName.trim(),
        email: form.email.trim(),
        mobileNumber: form.mobileNumber.trim(),
        dateOfBirth: form.dateOfBirth,
        address: form.address.trim(),
        password: form.password,
        confirmPassword: form.confirmPassword,
        transactionPin: form.transactionPin,
      });
      notify('Account created. Please sign in with your new credentials.', 'success');
      navigate('/login', { replace: true });
    } catch (err) {
      setServerError(apiErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="hfdc-auth-page">
      <div className="container">
        <div className="row g-0 hfdc-auth-shell hfdc-auth-shell-wide">
          <div className="col-lg-5 hfdc-auth-side">
            <Link to="/" className="text-decoration-none">
              <Logo size={46} light />
            </Link>
            <h1>Open your demo account</h1>
            <p>
              It takes less than a minute. You will get a savings account, a zero opening balance
              and full access to every simulated feature.
            </p>
            <ul className="hfdc-auth-points">
              <li><ShieldCheck size={17} /> Password hashed with BCrypt(12)</li>
              <li><ShieldCheck size={17} /> Separate transaction PIN for money movement</li>
              <li><ShieldCheck size={17} /> No real identity documents required</li>
            </ul>
            <div className="hfdc-auth-demo">
              <strong>Educational project</strong>
              <span>No real money or banking services are involved.</span>
            </div>
          </div>

          <div className="col-lg-7 hfdc-auth-form-wrap">
            <div className="hfdc-auth-form">
              <h2>Create your account</h2>
              <p className="hfdc-form-sub">All fields are required.</p>

              {serverError && (
                <div className="alert alert-danger d-flex align-items-start gap-2" role="alert">
                  <AlertCircle size={18} className="flex-shrink-0 mt-1" />
                  <span>{serverError}</span>
                </div>
              )}

              <form onSubmit={submit} noValidate>
                <div className="row g-3">
                  <div className="col-md-6">
                    <label className="form-label" htmlFor="reg-name">Full name</label>
                    <input
                      id="reg-name"
                      className={`form-control ${errors.fullName ? 'is-invalid' : ''}`}
                      value={form.fullName}
                      onChange={update('fullName')}
                      autoComplete="name"
                    />
                    {errors.fullName && <div className="invalid-feedback">{errors.fullName}</div>}
                  </div>

                  <div className="col-md-6">
                    <label className="form-label" htmlFor="reg-email">Email address</label>
                    <input
                      id="reg-email"
                      type="email"
                      className={`form-control ${errors.email ? 'is-invalid' : ''}`}
                      value={form.email}
                      onChange={update('email')}
                      autoComplete="email"
                    />
                    {errors.email && <div className="invalid-feedback">{errors.email}</div>}
                  </div>

                  <div className="col-md-6">
                    <label className="form-label" htmlFor="reg-mobile">Mobile number</label>
                    <input
                      id="reg-mobile"
                      inputMode="numeric"
                      maxLength={10}
                      className={`form-control ${errors.mobileNumber ? 'is-invalid' : ''}`}
                      value={form.mobileNumber}
                      onChange={(event) =>
                        setForm((current) => ({
                          ...current,
                          mobileNumber: event.target.value.replace(/\D/g, '').slice(0, 10),
                        }))
                      }
                      autoComplete="tel"
                      placeholder="10 digits"
                    />
                    {errors.mobileNumber && (
                      <div className="invalid-feedback">{errors.mobileNumber}</div>
                    )}
                  </div>

                  <div className="col-md-6">
                    <label className="form-label" htmlFor="reg-dob">Date of birth</label>
                    <input
                      id="reg-dob"
                      type="date"
                      className={`form-control ${errors.dateOfBirth ? 'is-invalid' : ''}`}
                      value={form.dateOfBirth}
                      onChange={update('dateOfBirth')}
                      max={new Date().toISOString().slice(0, 10)}
                    />
                    {errors.dateOfBirth && (
                      <div className="invalid-feedback">{errors.dateOfBirth}</div>
                    )}
                  </div>

                  <div className="col-12">
                    <label className="form-label" htmlFor="reg-address">Address</label>
                    <textarea
                      id="reg-address"
                      rows={2}
                      className={`form-control ${errors.address ? 'is-invalid' : ''}`}
                      value={form.address}
                      onChange={update('address')}
                      autoComplete="street-address"
                    />
                    {errors.address && <div className="invalid-feedback">{errors.address}</div>}
                  </div>

                  <div className="col-md-6">
                    <label className="form-label" htmlFor="reg-password">Password</label>
                    <div className="hfdc-input-group">
                      <input
                        id="reg-password"
                        type={showPassword ? 'text' : 'password'}
                        className={`form-control ${errors.password ? 'is-invalid' : ''}`}
                        value={form.password}
                        onChange={update('password')}
                        autoComplete="new-password"
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
                    {form.password && (
                      <div className={`hfdc-strength hfdc-strength-${strength.tone}`}>
                        <div className="hfdc-strength-bar">
                          <span style={{ width: `${(strength.score / 5) * 100}%` }} />
                        </div>
                        <small>{strength.label}</small>
                      </div>
                    )}
                    {errors.password && <div className="invalid-feedback d-block">{errors.password}</div>}
                  </div>

                  <div className="col-md-6">
                    <label className="form-label" htmlFor="reg-confirm">Confirm password</label>
                    <input
                      id="reg-confirm"
                      type={showPassword ? 'text' : 'password'}
                      className={`form-control ${errors.confirmPassword ? 'is-invalid' : ''}`}
                      value={form.confirmPassword}
                      onChange={update('confirmPassword')}
                      autoComplete="new-password"
                    />
                    {errors.confirmPassword && (
                      <div className="invalid-feedback">{errors.confirmPassword}</div>
                    )}
                  </div>

                  <div className="col-md-6">
                    <label className="form-label" htmlFor="reg-pin">Transaction PIN</label>
                    <input
                      id="reg-pin"
                      inputMode="numeric"
                      maxLength={6}
                      className={`form-control ${errors.transactionPin ? 'is-invalid' : ''}`}
                      value={form.transactionPin}
                      onChange={(event) =>
                        setForm((current) => ({
                          ...current,
                          transactionPin: event.target.value.replace(/\D/g, '').slice(0, 6),
                        }))
                      }
                      placeholder="4-6 digits"
                      autoComplete="off"
                    />
                    {errors.transactionPin && (
                      <div className="invalid-feedback">{errors.transactionPin}</div>
                    )}
                    <div className="form-text">Used to authorise withdrawals and transfers.</div>
                  </div>
                </div>

                <div className="hfdc-password-rules">
                  <CheckCircle2 size={15} />
                  <span>At least 8 characters, with upper case, lower case, a number and a symbol.</span>
                </div>

                <button type="submit" className="btn btn-primary btn-lg w-100 hfdc-btn mt-3" disabled={busy}>
                  {busy ? 'Creating account…' : (<><UserPlus size={18} /> Create demo account</>)}
                </button>
              </form>

              <p className="hfdc-auth-switch">
                Already registered? <Link to="/login">Sign in <ArrowRight size={14} /></Link>
              </p>

              <Disclaimer compact />
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
