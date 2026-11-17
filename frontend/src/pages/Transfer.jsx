import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  AlertCircle,
  ArrowRight,
  CheckCircle2,
  KeyRound,
  Send,
  ShieldCheck,
  UserRound,
  Users,
  Wallet,
} from 'lucide-react';
import { beneficiaryService } from '../services/beneficiaryService';
import { transferService } from '../services/transferService';
import { accountService } from '../services/accountService';
import { apiErrorMessage } from '../services/api';
import { formatMoney } from '../utils/format';
import { useNotify } from '../context/NotificationContext';
import PageHeader from '../components/PageHeader';
import ConfirmDialog from '../components/ConfirmDialog';
import Loading from '../components/Loading';

const INITIAL = {
  beneficiaryAccountNumber: '',
  beneficiaryName: '',
  amount: '',
  remarks: '',
  transactionPin: '',
};

/** Atomic peer-to-peer transfer with beneficiary shortcuts and a confirm step. */
export default function Transfer() {
  const [form, setForm] = useState(INITIAL);
  const [beneficiaries, setBeneficiaries] = useState([]);
  const [balance, setBalance] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [receipt, setReceipt] = useState(null);
  const { notify } = useNotify();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [saved, account] = await Promise.all([
        beneficiaryService.list(),
        accountService.balance(),
      ]);
      setBeneficiaries(saved);
      setBalance(account);
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const update = (field) => (event) => {
    setForm((current) => ({ ...current, [field]: event.target.value }));
    setError('');
  };

  const pickBeneficiary = (beneficiary) => {
    setForm((current) => ({
      ...current,
      beneficiaryAccountNumber: beneficiary.beneficiaryAccountNumber,
      beneficiaryName: beneficiary.beneficiaryName,
    }));
    setError('');
  };

  const review = (event) => {
    event.preventDefault();
    const amount = Number(form.amount);
    if (!form.beneficiaryAccountNumber.trim()) {
      setError('Enter or select a beneficiary account number.');
      return;
    }
    if (!form.beneficiaryName.trim()) {
      setError('Enter the beneficiary name.');
      return;
    }
    if (!Number.isFinite(amount) || amount <= 0) {
      setError('Enter a transfer amount greater than zero.');
      return;
    }
    if (amount > 100000) {
      setError('The transfer limit is ₹1,00,000 per transaction.');
      return;
    }
    if (!/^\d{4,6}$/.test(form.transactionPin)) {
      setError('Your transaction PIN must be 4 to 6 digits.');
      return;
    }
    if (balance && amount > Number(balance.balance)) {
      setError('This amount is higher than your available balance.');
      return;
    }
    setConfirmOpen(true);
  };

  const execute = async () => {
    setBusy(true);
    setError('');
    try {
      const data = await transferService.transfer({
        beneficiaryAccountNumber: form.beneficiaryAccountNumber.trim(),
        beneficiaryName: form.beneficiaryName.trim(),
        amount: Number(form.amount),
        remarks: form.remarks.trim() || undefined,
        transactionPin: form.transactionPin,
      });
      setReceipt({ ...data, amount: Number(form.amount) });
      setForm(INITIAL);
      setConfirmOpen(false);
      notify('Transfer completed successfully.', 'success');
      load();
    } catch (err) {
      setConfirmOpen(false);
      setError(apiErrorMessage(err));
      setForm((current) => ({ ...current, transactionPin: '' }));
    } finally {
      setBusy(false);
    }
  };

  if (loading) return <Loading label="Loading beneficiaries and balance…" />;

  return (
    <>
      <PageHeader
        title="Fund transfer"
        subtitle="Move simulated money to another HFDC account in a single atomic transaction."
      />

      <div className="row g-4">
        <div className="col-lg-7">
          <div className="hfdc-form-card">
            {receipt ? (
              <div className="hfdc-success">
                <div className="hfdc-success-icon">
                  <CheckCircle2 size={30} />
                </div>
                <h3>Transfer completed</h3>
                <p>Both accounts were updated inside one database transaction.</p>

                <dl className="hfdc-receipt">
                  <div><dt>Reference</dt><dd className="hfdc-mono">{receipt.referenceNumber}</dd></div>
                  <div><dt>Amount</dt><dd className="text-danger">{formatMoney(receipt.amount)}</dd></div>
                  <div><dt>To</dt><dd>{receipt.beneficiaryName}</dd></div>
                  <div><dt>Account</dt><dd className="hfdc-mono">{receipt.beneficiaryAccountNumber}</dd></div>
                  <div><dt>New balance</dt><dd>{formatMoney(receipt.balance)}</dd></div>
                </dl>

                <div className="d-flex flex-column flex-sm-row gap-2 justify-content-center mt-4">
                  <button type="button" className="btn btn-primary hfdc-btn" onClick={() => setReceipt(null)}>
                    New transfer
                  </button>
                  <Link to="/transactions" className="btn btn-outline-primary hfdc-btn">
                    View history
                  </Link>
                </div>
              </div>
            ) : (
              <form onSubmit={review} noValidate>
                <h2 className="hfdc-form-title">
                  <Send size={20} /> New transfer
                </h2>
                <p className="hfdc-form-sub">
                  Available balance: <strong>{balance ? formatMoney(balance.balance) : '—'}</strong>
                </p>

                {error && (
                  <div className="alert alert-danger d-flex align-items-start gap-2" role="alert">
                    <AlertCircle size={18} className="flex-shrink-0 mt-1" />
                    <span>{error}</span>
                  </div>
                )}

                {beneficiaries.length > 0 && (
                  <div className="mb-4">
                    <span className="form-label d-block">Saved beneficiaries</span>
                    <div className="hfdc-beneficiary-picks">
                      {beneficiaries.slice(0, 4).map((beneficiary) => {
                        const active =
                          form.beneficiaryAccountNumber === beneficiary.beneficiaryAccountNumber;
                        return (
                          <button
                            key={beneficiary.beneficiaryId}
                            type="button"
                            className={`hfdc-beneficiary-pick ${active ? 'active' : ''}`}
                            onClick={() => pickBeneficiary(beneficiary)}
                          >
                            <span className="hfdc-avatar hfdc-avatar-soft">
                              {beneficiary.beneficiaryName[0]}
                            </span>
                            <span>
                              <strong>{beneficiary.beneficiaryName}</strong>
                              <small className="hfdc-mono">{beneficiary.beneficiaryAccountNumber}</small>
                            </span>
                          </button>
                        );
                      })}
                    </div>
                  </div>
                )}

                <div className="row g-3">
                  <div className="col-md-6">
                    <label className="form-label" htmlFor="transfer-account">Beneficiary account</label>
                    <input
                      id="transfer-account"
                      className="form-control hfdc-mono"
                      value={form.beneficiaryAccountNumber}
                      onChange={update('beneficiaryAccountNumber')}
                      placeholder="HFDC1000000002"
                      disabled={busy}
                    />
                  </div>
                  <div className="col-md-6">
                    <label className="form-label" htmlFor="transfer-name">Beneficiary name</label>
                    <input
                      id="transfer-name"
                      className="form-control"
                      value={form.beneficiaryName}
                      onChange={update('beneficiaryName')}
                      placeholder="Priya Nair"
                      disabled={busy}
                    />
                  </div>
                  <div className="col-md-6">
                    <label className="form-label" htmlFor="transfer-amount">Amount (₹)</label>
                    <div className="input-group">
                      <span className="input-group-text">₹</span>
                      <input
                        id="transfer-amount"
                        type="number"
                        min="0.01"
                        max="100000"
                        step="0.01"
                        className="form-control"
                        value={form.amount}
                        onChange={update('amount')}
                        placeholder="0.00"
                        disabled={busy}
                      />
                    </div>
                  </div>
                  <div className="col-md-6">
                    <label className="form-label" htmlFor="transfer-pin">
                      <KeyRound size={14} /> Transaction PIN
                    </label>
                    <input
                      id="transfer-pin"
                      type="password"
                      inputMode="numeric"
                      maxLength={6}
                      className="form-control"
                      value={form.transactionPin}
                      onChange={(event) =>
                        setForm((current) => ({
                          ...current,
                          transactionPin: event.target.value.replace(/\D/g, '').slice(0, 6),
                        }))
                      }
                      autoComplete="off"
                      placeholder="••••"
                      disabled={busy}
                    />
                  </div>
                  <div className="col-12">
                    <label className="form-label" htmlFor="transfer-remarks">
                      Remarks <span className="text-muted">(optional)</span>
                    </label>
                    <input
                      id="transfer-remarks"
                      className="form-control"
                      maxLength={120}
                      value={form.remarks}
                      onChange={update('remarks')}
                      placeholder="e.g. Rent share"
                      disabled={busy}
                    />
                  </div>
                </div>

                <button type="submit" className="btn btn-primary btn-lg hfdc-btn mt-4" disabled={busy}>
                  <Send size={18} /> Review transfer
                </button>
              </form>
            )}
          </div>
        </div>

        <div className="col-lg-5">
          <div className="hfdc-panel">
            <h3 className="hfdc-panel-title">
              <ShieldCheck size={18} /> Atomic by design
            </h3>
            <ul className="hfdc-check-list">
              <li>Both account rows are locked in ascending id order.</li>
              <li>The debit only runs when the balance is sufficient.</li>
              <li>Two ledger rows plus an audit row are written together.</li>
              <li>Any failure rolls the whole transfer back.</li>
            </ul>
            <div className="hfdc-security-note mt-3">
              <Users size={18} />
              <p>
                Transferring to another HFDC customer? Use account{' '}
                <span className="hfdc-mono">HFDC1000000002</span> (Priya, PIN 4321) or{' '}
                <span className="hfdc-mono">HFDC1000000001</span> (Aarav, PIN 1234).
              </p>
            </div>
            <Link to="/beneficiaries" className="btn btn-sm btn-outline-primary hfdc-btn mt-3">
              <UserRound size={15} /> Manage beneficiaries <ArrowRight size={14} />
            </Link>
          </div>

          {balance && (
            <div className="hfdc-mini-balance mt-4">
              <Wallet size={20} />
              <div>
                <span>Available to transfer</span>
                <strong>{formatMoney(balance.balance)}</strong>
              </div>
            </div>
          )}
        </div>
      </div>

      <ConfirmDialog
        open={confirmOpen}
        title="Confirm this transfer"
        message={`Send ${formatMoney(form.amount)} to ${form.beneficiaryName} (${form.beneficiaryAccountNumber})? This will immediately update both balances.`}
        confirmLabel="Send transfer"
        busy={busy}
        onCancel={() => setConfirmOpen(false)}
        onConfirm={execute}
      />
    </>
  );
}
