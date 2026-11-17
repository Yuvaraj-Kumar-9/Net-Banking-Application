import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  AlertCircle,
  CheckCircle2,
  KeyRound,
  Mail,
  MapPin,
  Phone,
  RefreshCw,
  Save,
  ShieldCheck,
  UserRound,
} from 'lucide-react';
import { profileService } from '../services/profileService';
import { apiErrorMessage } from '../services/api';
import { formatDate } from '../utils/format';
import { useAuth } from '../context/AuthContext';
import { useNotify } from '../context/NotificationContext';
import PageHeader from '../components/PageHeader';
import Loading from '../components/Loading';

/** Profile page. Email, customer id and account number are read-only. */
export default function Profile() {
  const [profile, setProfile] = useState(null);
  const [form, setForm] = useState({ fullName: '', mobileNumber: '', address: '' });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const { refresh } = useAuth();
  const { notify } = useNotify();

  const apply = (data) => {
    setProfile(data);
    setForm({
      fullName: data.fullName || '',
      mobileNumber: data.mobileNumber || '',
      address: data.address || '',
    });
  };

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      apply(await profileService.get());
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const update = (field) => (event) => {
    setForm((current) => ({ ...current, [field]: event.target.value }));
    setFormError('');
    setSaved(false);
  };

  const submit = async (event) => {
    event.preventDefault();
    if (!form.fullName.trim()) {
      setFormError('Full name is required.');
      return;
    }
    if (!/^[0-9]{10}$/.test(form.mobileNumber.trim())) {
      setFormError('Mobile number must be exactly 10 digits.');
      return;
    }
    if (!form.address.trim()) {
      setFormError('Address is required.');
      return;
    }
    setSaving(true);
    setFormError('');
    try {
      const data = await profileService.update({
        fullName: form.fullName.trim(),
        mobileNumber: form.mobileNumber.trim(),
        address: form.address.trim(),
      });
      apply(data);
      setSaved(true);
      notify('Profile updated successfully.', 'success');
      // Keep the sidebar name in sync with the new value.
      refresh();
    } catch (err) {
      setFormError(apiErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <Loading label="Loading your profile…" />;

  if (error) {
    return (
      <div className="hfdc-error-state">
        <AlertCircle size={32} />
        <h2>Profile unavailable</h2>
        <p>{error}</p>
        <button type="button" className="btn btn-primary hfdc-btn" onClick={load}>
          <RefreshCw size={16} /> Retry
        </button>
      </div>
    );
  }

  return (
    <>
      <PageHeader
        title="Profile"
        subtitle="Keep your contact details current. Your email address cannot be changed."
        actions={
          <Link to="/change-password" className="btn btn-outline-primary hfdc-btn">
            <KeyRound size={16} /> Change password
          </Link>
        }
      />

      <div className="row g-4">
        <div className="col-lg-7">
          <div className="hfdc-form-card">
            <h2 className="hfdc-form-title">
              <UserRound size={20} /> Personal details
            </h2>

            {formError && (
              <div className="alert alert-danger d-flex align-items-start gap-2" role="alert">
                <AlertCircle size={18} className="flex-shrink-0 mt-1" />
                <span>{formError}</span>
              </div>
            )}

            {saved && (
              <div className="alert alert-success d-flex align-items-center gap-2" role="status">
                <CheckCircle2 size={18} />
                <span>Your changes have been saved.</span>
              </div>
            )}

            <form onSubmit={submit} noValidate className="row g-3">
              <div className="col-12">
                <label className="form-label" htmlFor="profile-name">Full name</label>
                <input
                  id="profile-name"
                  className="form-control"
                  value={form.fullName}
                  onChange={update('fullName')}
                  autoComplete="name"
                  disabled={saving}
                />
              </div>

              <div className="col-md-6">
                <label className="form-label" htmlFor="profile-email">Email address</label>
                <input
                  id="profile-email"
                  className="form-control"
                  value={profile.email}
                  readOnly
                  disabled
                />
                <div className="form-text">Email is your login identity and cannot be edited.</div>
              </div>

              <div className="col-md-6">
                <label className="form-label" htmlFor="profile-mobile">
                  <Phone size={14} /> Mobile number
                </label>
                <input
                  id="profile-mobile"
                  inputMode="numeric"
                  maxLength={10}
                  className="form-control"
                  value={form.mobileNumber}
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      mobileNumber: event.target.value.replace(/\D/g, '').slice(0, 10),
                    }))
                  }
                  autoComplete="tel"
                  disabled={saving}
                />
              </div>

              <div className="col-12">
                <label className="form-label" htmlFor="profile-address">
                  <MapPin size={14} /> Address
                </label>
                <textarea
                  id="profile-address"
                  rows={3}
                  className="form-control"
                  value={form.address}
                  onChange={update('address')}
                  autoComplete="street-address"
                  disabled={saving}
                />
              </div>

              <div className="col-12 d-flex flex-wrap gap-2">
                <button type="submit" className="btn btn-primary hfdc-btn" disabled={saving}>
                  {saving ? 'Saving…' : (<><Save size={16} /> Save changes</>)}
                </button>
                <button
                  type="button"
                  className="btn btn-light hfdc-btn"
                  onClick={() => apply(profile)}
                  disabled={saving}
                >
                  Reset form
                </button>
              </div>
            </form>
          </div>
        </div>

        <div className="col-lg-5">
          <div className="hfdc-panel">
            <h3 className="hfdc-panel-title">
              <ShieldCheck size={18} /> Account identity
            </h3>
            <dl className="hfdc-detail-list">
              <div><dt>Customer ID</dt><dd className="hfdc-mono">{profile.customerId}</dd></div>
              <div><dt>Account number</dt><dd className="hfdc-mono">{profile.accountNumber}</dd></div>
              <div><dt>Date of birth</dt><dd>{formatDate(profile.dateOfBirth)}</dd></div>
              <div><dt>Member since</dt><dd>{formatDate(profile.createdAt)}</dd></div>
              <div>
                <dt>Email</dt>
                <dd className="d-flex align-items-center gap-2">
                  <Mail size={14} /> {profile.email}
                </dd>
              </div>
            </dl>

            <div className="hfdc-security-note">
              <ShieldCheck size={18} />
              <p>
                Customer ID, account number and date of birth are fixed identity fields. Only your
                name, mobile number and address can be edited.
              </p>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
