import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  AlertCircle,
  Eye,
  EyeOff,
  RefreshCw,
  Wallet,
  ArrowDownToLine,
  Send,
  ReceiptText,
  Clock3,
} from 'lucide-react';
import { accountService } from '../services/accountService';
import { apiErrorMessage } from '../services/api';
import { formatDateTime, formatMoney } from '../utils/format';
import Loading from '../components/Loading';
import PageHeader from '../components/PageHeader';

/** Balance enquiry with a privacy toggle and manual refresh. */
export default function Balance() {
  const [balance, setBalance] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [hidden, setHidden] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setBalance(await accountService.balance());
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  if (loading) return <Loading label="Fetching your balance…" />;

  if (error) {
    return (
      <div className="hfdc-error-state">
        <AlertCircle size={32} />
        <h2>Balance unavailable</h2>
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
        title="Balance enquiry"
        subtitle="Your available balance, read live from MySQL."
        actions={
          <button type="button" className="btn btn-outline-primary hfdc-btn" onClick={load}>
            <RefreshCw size={16} /> Refresh balance
          </button>
        }
      />

      <div className="row g-4">
        <div className="col-lg-7">
          <div className="hfdc-balance-hero">
            <div className="d-flex justify-content-between align-items-start">
              <div>
                <span className="hfdc-balance-label">
                  <Wallet size={16} /> Available balance
                </span>
                <div className="hfdc-balance-value">
                  {hidden ? '••••••••' : formatMoney(balance.balance)}
                </div>
                <span className="hfdc-balance-account hfdc-mono">{balance.accountNumber}</span>
              </div>
              <button
                type="button"
                className="btn btn-sm btn-light hfdc-btn"
                onClick={() => setHidden((value) => !value)}
                aria-label={hidden ? 'Show balance' : 'Hide balance'}
              >
                {hidden ? <Eye size={16} /> : <EyeOff size={16} />}
                {hidden ? 'Show' : 'Hide'}
              </button>
            </div>

            <div className="hfdc-balance-foot">
              <div>
                <span>Account type</span>
                <strong>{balance.accountType}</strong>
              </div>
              <div>
                <span><Clock3 size={13} /> Last ledger update</span>
                <strong>{formatDateTime(balance.lastUpdated)}</strong>
              </div>
            </div>
          </div>
        </div>

        <div className="col-lg-5">
          <div className="hfdc-panel h-100">
            <h3 className="hfdc-panel-title">What next?</h3>
            <p className="hfdc-panel-sub">
              Move simulated money and watch the balance update immediately.
            </p>
            <div className="d-grid gap-2">
              <Link to="/deposit" className="btn btn-primary hfdc-btn">
                <ArrowDownToLine size={16} /> Deposit money
              </Link>
              <Link to="/transfer" className="btn btn-outline-primary hfdc-btn">
                <Send size={16} /> Transfer funds
              </Link>
              <Link to="/transactions" className="btn btn-outline-primary hfdc-btn">
                <ReceiptText size={16} /> View transactions
              </Link>
            </div>
            <div className="hfdc-security-note mt-4">
              <Wallet size={18} />
              <p>
                Balances are stored as DECIMAL(19,2), so amounts never suffer floating-point
                rounding errors.
              </p>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
