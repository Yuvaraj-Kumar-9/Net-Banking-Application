import { useState } from 'react';
import { Link } from 'react-router-dom';
import {
  AlertCircle,
  ArrowDownToLine,
  CheckCircle2,
  Info,
  ReceiptText,
  Wallet,
} from 'lucide-react';
import { transactionService } from '../services/transactionService';
import { apiErrorMessage } from '../services/api';
import { formatMoney } from '../utils/format';
import { useNotify } from '../context/NotificationContext';
import PageHeader from '../components/PageHeader';

const QUICK_AMOUNTS = [500, 1000, 5000, 10000];

/** Simulated deposit form. No real payment method is involved. */
export default function Deposit() {
  const [amount, setAmount] = useState('');
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
    if (value > 1000000) {
      setError('The deposit limit is ₹10,00,000 per transaction.');
      return;
    }
    if (!/^\d+(\.\d{1,2})?$/.test(String(amount).trim())) {
      setError('Use at most two decimal places.');
      return;
    }

    setBusy(true);
    setError('');
    try {
      const data = await transactionService.deposit(value, description.trim() || undefined);
      // Keep the submitted amount for the receipt (the API returns the new balance).
      setReceipt({ ...data, amount: value });
      setAmount('');
      setDescription('');
      notify('Simulated deposit completed.', 'success');
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <PageHeader
        title="Deposit money"
        subtitle="Add simulated funds to your HFDC Bank account."
      />

      <div className="row g-4">
        <div className="col-lg-7">
          <div className="hfdc-form-card">
            {receipt ? (
              <div className="hfdc-success">
                <div className="hfdc-success-icon">
                  <CheckCircle2 size={30} />
                </div>
                <h3>Deposit successful</h3>
                <p>This was a simulated educational transaction. No real money moved.</p>

                <dl className="hfdc-receipt">
                  <div><dt>Reference</dt><dd className="hfdc-mono">{receipt.referenceNumber}</dd></div>
                  <div><dt>Amount</dt><dd className="text-success">{formatMoney(receipt.amount ?? receipt.balance)}</dd></div>
                  <div><dt>New balance</dt><dd>{formatMoney(receipt.balance)}</dd></div>
                </dl>

                <div className="d-flex flex-column flex-sm-row gap-2 justify-content-center mt-4">
                  <button type="button" className="btn btn-primary hfdc-btn" onClick={() => setReceipt(null)}>
                    Make another deposit
                  </button>
                  <Link to="/dashboard" className="btn btn-outline-primary hfdc-btn">
                    Back to dashboard
                  </Link>
                </div>
              </div>
            ) : (
              <form onSubmit={submit} noValidate>
                <h2 className="hfdc-form-title">
                  <ArrowDownToLine size={20} /> New deposit
                </h2>
                <p className="hfdc-form-sub">Maximum ₹10,00,000 per transaction.</p>

                {error && (
                  <div className="alert alert-danger d-flex align-items-start gap-2" role="alert">
                    <AlertCircle size={18} className="flex-shrink-0 mt-1" />
                    <span>{error}</span>
                  </div>
                )}

                <div className="mb-3">
                  <label className="form-label" htmlFor="deposit-amount">Amount (₹)</label>
                  <div className="input-group input-group-lg">
                    <span className="input-group-text">₹</span>
                    <input
                      id="deposit-amount"
                      type="number"
                      min="0.01"
                      max="1000000"
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
                        +{formatMoney(value)}
                      </button>
                    ))}
                  </div>
                </div>

                <div className="mb-4">
                  <label className="form-label" htmlFor="deposit-description">
                    Description <span className="text-muted">(optional)</span>
                  </label>
                  <input
                    id="deposit-description"
                    className="form-control"
                    maxLength={120}
                    value={description}
                    onChange={(event) => setDescription(event.target.value)}
                    placeholder="e.g. Pocket money"
                    disabled={busy}
                  />
                </div>

                <button type="submit" className="btn btn-primary btn-lg hfdc-btn" disabled={busy}>
                  {busy ? 'Processing…' : (<><Wallet size={18} /> Deposit {amount ? formatMoney(amount) : 'money'}</>)}
                </button>
              </form>
            )}
          </div>
        </div>

        <div className="col-lg-5">
          <div className="hfdc-panel">
            <h3 className="hfdc-panel-title">
              <Info size={18} /> How deposits work
            </h3>
            <ul className="hfdc-check-list">
              <li>The balance update and ledger entry share one database transaction.</li>
              <li>If the ledger write fails, the balance change is rolled back.</li>
              <li>Every deposit receives a unique reference number.</li>
              <li>No card, UPI or bank transfer is involved - this is a simulation.</li>
            </ul>
            <Link to="/transactions" className="btn btn-sm btn-outline-primary hfdc-btn mt-2">
              <ReceiptText size={15} /> See the ledger
            </Link>
          </div>
        </div>
      </div>
    </>
  );
}
