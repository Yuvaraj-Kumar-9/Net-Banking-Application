import { useState } from 'react';
import { Link } from 'react-router-dom';
import {
  AlertCircle,
  ArrowUpFromLine,
  CheckCircle2,
  KeyRound,
  Lock,
  ReceiptText,
} from 'lucide-react';
import { transactionService } from '../services/transactionService';
import { apiErrorMessage } from '../services/api';
import { formatMoney } from '../utils/format';
import { useNotify } from '../context/NotificationContext';
import PageHeader from '../components/PageHeader';

const QUICK_AMOUNTS = [500, 1000, 5000, 10000];

/** Withdrawal form. Requires the BCrypt-verified transaction PIN. */
export default function Withdraw() {
  const [amount, setAmount] = useState('');
  const [pin, setPin] = useState('');
  const [description, setDescription] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [receipt, setReceipt] = useState(null);
  const { notify } = useNotify();

  const submit = async (event) => {
    event.preventDefault();
    const value = Number(amount);
    if (!Number.isFinite(value) || value <= 0) {
      setError('Enter an amount greater than zero.');
      return;
    }
    if (value > 100000) {
      setError('The withdrawal limit is ₹1,00,000 per transaction.');
      return;
    }
    if (!/^\d{4,6}$/.test(pin)) {
      setError('Your transaction PIN must be 4 to 6 digits.');
      return;
    }

    setBusy(true);
    setError('');
    try {
      const data = await transactionService.withdraw(value, pin, description.trim() || undefined);
      setReceipt({ ...data, amount: value });
      setAmount('');
      setPin('');
      setDescription('');
      notify('Withdrawal completed.', 'success');
    } catch (err) {
      setError(apiErrorMessage(err));
      setPin('');
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <PageHeader
        title="Withdraw money"
        subtitle="Authorise a simulated cash withdrawal with your transaction PIN."
      />

      <div className="row g-4">
        <div className="col-lg-7">
          <div className="hfdc-form-card">
            {receipt ? (
              <div className="hfdc-success">
                <div className="hfdc-success-icon">
                  <CheckCircle2 size={30} />
                </div>
                <h3>Withdrawal successful</h3>
                <p>Your transaction PIN was verified and the debit was atomic.</p>

                <dl className="hfdc-receipt">
                  <div><dt>Reference</dt><dd className="hfdc-mono">{receipt.referenceNumber}</dd></div>
                  <div><dt>Amount</dt><dd className="text-danger">{formatMoney(receipt.amount)}</dd></div>
                  <div><dt>New balance</dt><dd>{formatMoney(receipt.balance)}</dd></div>
                </dl>

                <div className="d-flex flex-column flex-sm-row gap-2 justify-content-center mt-4">
                  <button type="button" className="btn btn-primary hfdc-btn" onClick={() => setReceipt(null)}>
                    Withdraw again
                  </button>
                  <Link to="/dashboard" className="btn btn-outline-primary hfdc-btn">
                    Back to dashboard
                  </Link>
                </div>
              </div>
            ) : (
              <form onSubmit={submit} noValidate>
                <h2 className="hfdc-form-title">
                  <ArrowUpFromLine size={20} /> New withdrawal
                </h2>
                <p className="hfdc-form-sub">
                  Maximum ₹1,00,000 per transaction. Your PIN is never stored in the browser.
                </p>

                {error && (
                  <div className="alert alert-danger d-flex align-items-start gap-2" role="alert">
                    <AlertCircle size={18} className="flex-shrink-0 mt-1" />
                    <span>{error}</span>
                  </div>
                )}

                <div className="mb-3">
                  <label className="form-label" htmlFor="withdraw-amount">Amount (₹)</label>
                  <div className="input-group input-group-lg">
                    <span className="input-group-text">₹</span>
                    <input
                      id="withdraw-amount"
                      type="number"
                      min="0.01"
                      max="100000"
                      step="0.01"
                      className="form-control"
                      value={amount}
                      onChange={(event) => {
                        setAmount(event.target.value);
                        setError('');
                      }}
                      placeholder="0.00"
                      disabled={busy}
                    />
                  </div>
                  <div className="hfdc-chip-row">
                    {QUICK_AMOUNTS.map((value) => (
                      <button
                        key={value}
                        type="button"
                        className="btn btn-sm btn-outline-primary hfdc-chip"
                        onClick={() => setAmount(String(value))}
                        disabled={busy}
                      >
                        {formatMoney(value)}
                      </button>
                    ))}
                  </div>
                </div>

                <div className="mb-3">
                  <label className="form-label" htmlFor="withdraw-pin">
                    <KeyRound size={14} /> Transaction PIN
                  </label>
                  <input
                    id="withdraw-pin"
                    type="password"
                    inputMode="numeric"
                    maxLength={6}
                    className="form-control form-control-lg"
                    value={pin}
                    onChange={(event) => {
                      setPin(event.target.value.replace(/\D/g, '').slice(0, 6));
                      setError('');
                    }}
                    autoComplete="off"
                    placeholder="••••"
                    disabled={busy}
                  />
                  <div className="form-text">Demo PINs: 1234 (Aarav) · 4321 (Priya)</div>
                </div>

                <div className="mb-4">
                  <label className="form-label" htmlFor="withdraw-description">
                    Description <span className="text-muted">(optional)</span>
                  </label>
                  <input
                    id="withdraw-description"
                    className="form-control"
                    maxLength={120}
                    value={description}
                    onChange={(event) => setDescription(event.target.value)}
                    placeholder="e.g. ATM withdrawal"
                    disabled={busy}
                  />
                </div>

                <button type="submit" className="btn btn-primary btn-lg hfdc-btn" disabled={busy}>
                  {busy ? 'Verifying…' : (<><Lock size={18} /> Withdraw {amount ? formatMoney(amount) : 'money'}</>)}
                </button>
              </form>
            )}
          </div>
        </div>

        <div className="col-lg-5">
          <div className="hfdc-panel">
            <h3 className="hfdc-panel-title">
              <Lock size={18} /> Withdrawal safeguards
            </h3>
            <ul className="hfdc-check-list">
              <li>The PIN is checked against a BCrypt hash, not plain text.</li>
              <li>SQL only debits when the balance is sufficient at update time.</li>
              <li>Failed PIN attempts never move money.</li>
              <li>The ledger row and debit share one transaction.</li>
            </ul>
            <Link to="/transactions" className="btn btn-sm btn-outline-primary hfdc-btn mt-2">
              <ReceiptText size={15} /> Review withdrawals
            </Link>
          </div>
        </div>
      </div>
    </>
  );
}
