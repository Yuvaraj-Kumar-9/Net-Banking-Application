import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  AlertCircle,
  Check,
  Copy,
  Landmark,
  RefreshCw,
  ShieldCheck,
  Wallet,
  ArrowRight,
} from 'lucide-react';
import { accountService } from '../services/accountService';
import { apiErrorMessage } from '../services/api';
import { formatDateTime, formatMoney } from '../utils/format';
import Loading from '../components/Loading';
import PageHeader from '../components/PageHeader';
import { useNotify } from '../context/NotificationContext';

/** Read-only account details page. */
export default function Account() {
  const [account, setAccount] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [copied, setCopied] = useState(false);
  const { notify } = useNotify();

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setAccount(await accountService.myAccount());
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const copyNumber = async () => {
    try {
      await navigator.clipboard.writeText(account.accountNumber);
      setCopied(true);
      notify('Account number copied to clipboard.', 'success');
      window.setTimeout(() => setCopied(false), 2500);
    } catch {
      notify('Copy is not available in this browser.', 'warning');
    }
  };

  if (loading) return <Loading label="Loading account details…" />;

  if (error) {
    return (
      <div className="hfdc-error-state">
        <AlertCircle size={32} />
        <h2>Account details unavailable</h2>
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
        title="My account"
        subtitle="Your customer and account information, read directly from the bank database."
        actions={
          <button type="button" className="btn btn-outline-primary hfdc-btn" onClick={load}>
            <RefreshCw size={16} /> Refresh
          </button>
        }
      />

      <div className="row g-4">
        <div className="col-lg-7">
          <div className="hfdc-account-card">
            <div className="hfdc-account-card-top">
              <div>
                <span className="hfdc-account-label">HFDC Bank · {account.accountType}</span>
                <h2>{formatMoney(account.balance)}</h2>
                <span className="hfdc-account-sub">Available balance</span>
              </div>
              <Landmark size={40} className="hfdc-account-emblem" />
            </div>
            <div className="hfdc-account-number-row">
              <span className="hfdc-mono hfdc-account-number">{account.accountNumber}</span>
              <button type="button" className="btn btn-sm btn-light hfdc-btn" onClick={copyNumber}>
                {copied ? <Check size={15} /> : <Copy size={15} />}
                {copied ? 'Copied' : 'Copy'}
              </button>
            </div>
            <div className="hfdc-account-card-foot">
              <div>
                <span>Customer ID</span>
                <strong>{account.customerId}</strong>
              </div>
              <div>
                <span>Status</span>
                <strong className="text-success">{account.accountStatus}</strong>
              </div>
              <div>
                <span>Opened</span>
                <strong>{formatDateTime(account.createdAt)}</strong>
              </div>
            </div>
          </div>
        </div>

        <div className="col-lg-5">
          <div className="hfdc-panel h-100">
            <h3 className="hfdc-panel-title">Account summary</h3>
            <dl className="hfdc-detail-list">
              <div><dt>Account holder</dt><dd>—</dd></div>
              <div><dt>Account number</dt><dd className="hfdc-mono">{account.accountNumber}</dd></div>
              <div><dt>Account type</dt><dd>{account.accountType}</dd></div>
              <div><dt>Currency</dt><dd>INR (simulated)</dd></div>
              <div><dt>Available balance</dt><dd>{formatMoney(account.balance)}</dd></div>
              <div><dt>Status</dt><dd><span className="hfdc-badge hfdc-badge-success">{account.accountStatus}</span></dd></div>
            </dl>

            <div className="hfdc-security-note">
              <ShieldCheck size={18} />
              <p>
                This page is read-only. Account numbers and balances are resolved from your JWT
                session, never from a value supplied by the browser.
              </p>
            </div>

            <div className="d-flex flex-wrap gap-2 mt-3">
              <Link to="/balance" className="btn btn-sm btn-outline-primary hfdc-btn">
                <Wallet size={15} /> Balance enquiry
              </Link>
              <Link to="/profile" className="btn btn-sm btn-outline-primary hfdc-btn">
                Edit profile <ArrowRight size={15} />
              </Link>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
