import { useCallback, useEffect, useState } from 'react';
import {
  AlertCircle,
  Plus,
  Search,
  Trash2,
  UserPlus,
  Users,
  RefreshCw,
} from 'lucide-react';
import { beneficiaryService } from '../services/beneficiaryService';
import { apiErrorMessage } from '../services/api';
import { formatDate } from '../utils/format';
import { useNotify } from '../context/NotificationContext';
import PageHeader from '../components/PageHeader';
import Loading from '../components/Loading';
import ConfirmDialog from '../components/ConfirmDialog';

/** Beneficiary CRUD with search, duplicate protection and a confirm delete. */
export default function Beneficiaries() {
  const [list, setList] = useState([]);
  const [searchInput, setSearchInput] = useState('');
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [formOpen, setFormOpen] = useState(false);
  const [form, setForm] = useState({ name: '', accountNumber: '' });
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);
  const [pendingDelete, setPendingDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const { notify } = useNotify();

  useEffect(() => {
    const timer = window.setTimeout(() => setSearch(searchInput.trim()), 300);
    return () => window.clearTimeout(timer);
  }, [searchInput]);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setList(await beneficiaryService.list(search));
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [search]);

  useEffect(() => {
    load();
  }, [load]);

  const add = async (event) => {
    event.preventDefault();
    if (!form.name.trim()) {
      setFormError('Enter the beneficiary name.');
      return;
    }
    if (!form.accountNumber.trim()) {
      setFormError('Enter the beneficiary account number.');
      return;
    }
    setSaving(true);
    setFormError('');
    try {
      await beneficiaryService.add(form.name.trim(), form.accountNumber.trim());
      notify('Beneficiary saved.', 'success');
      setForm({ name: '', accountNumber: '' });
      setFormOpen(false);
      load();
    } catch (err) {
      setFormError(apiErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const remove = async () => {
    if (!pendingDelete) return;
    setDeleting(true);
    try {
      await beneficiaryService.remove(pendingDelete.beneficiaryId);
      notify('Beneficiary removed.', 'info');
      setPendingDelete(null);
      load();
    } catch (err) {
      notify(apiErrorMessage(err), 'danger');
    } finally {
      setDeleting(false);
    }
  };

  return (
    <>
      <PageHeader
        title="Beneficiaries"
        subtitle="Save the HFDC accounts you transfer to most often."
        actions={
          <>
            <button type="button" className="btn btn-outline-primary hfdc-btn" onClick={load}>
              <RefreshCw size={16} /> Refresh
            </button>
            <button
              type="button"
              className="btn btn-primary hfdc-btn"
              onClick={() => {
                setFormOpen((value) => !value);
                setFormError('');
              }}
            >
              <Plus size={16} /> Add beneficiary
            </button>
          </>
        }
      />

      {formOpen && (
        <div className="hfdc-form-card mb-4">
          <h2 className="hfdc-form-title">
            <UserPlus size={20} /> New beneficiary
          </h2>
          <p className="hfdc-form-sub">
            The account must already exist at HFDC Bank and cannot be your own account.
          </p>

          {formError && (
            <div className="alert alert-danger d-flex align-items-start gap-2" role="alert">
              <AlertCircle size={18} className="flex-shrink-0 mt-1" />
              <span>{formError}</span>
            </div>
          )}

          <form onSubmit={add} noValidate className="row g-3 align-items-end">
            <div className="col-md-5">
              <label className="form-label" htmlFor="beneficiary-name">Beneficiary name</label>
              <input
                id="beneficiary-name"
                className="form-control"
                value={form.name}
                onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))}
                placeholder="e.g. Priya Nair"
                disabled={saving}
              />
            </div>
            <div className="col-md-5">
              <label className="form-label" htmlFor="beneficiary-account">Account number</label>
              <input
                id="beneficiary-account"
                className="form-control hfdc-mono"
                value={form.accountNumber}
                onChange={(event) =>
                  setForm((current) => ({ ...current, accountNumber: event.target.value }))
                }
                placeholder="HFDC1000000002"
                disabled={saving}
              />
            </div>
            <div className="col-md-2 d-flex gap-2">
              <button type="submit" className="btn btn-primary hfdc-btn flex-grow-1" disabled={saving}>
                {saving ? 'Saving…' : 'Save'}
              </button>
              <button
                type="button"
                className="btn btn-light hfdc-btn"
                onClick={() => setFormOpen(false)}
                disabled={saving}
              >
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}

      <div className="hfdc-panel">
        <div className="hfdc-filter-bar hfdc-filter-bar-simple">
          <div className="hfdc-filter-search">
            <Search size={17} />
            <input
              type="search"
              className="form-control"
              placeholder="Search by name or account number…"
              value={searchInput}
              onChange={(event) => setSearchInput(event.target.value)}
              aria-label="Search beneficiaries"
            />
          </div>
          <span className="text-muted small">{list.length} saved</span>
        </div>

        {error && (
          <div className="alert alert-danger d-flex align-items-start gap-2 mx-3 mt-3" role="alert">
            <AlertCircle size={18} className="flex-shrink-0 mt-1" />
            <span>{error}</span>
          </div>
        )}

        {loading ? (
          <Loading label="Loading beneficiaries…" />
        ) : list.length === 0 ? (
          <div className="hfdc-empty">
            <Users size={30} />
            <h3>No beneficiaries yet</h3>
            <p>Add a recipient to make transfers faster.</p>
            <button
              type="button"
              className="btn btn-primary btn-sm hfdc-btn"
              onClick={() => setFormOpen(true)}
            >
              <Plus size={15} /> Add your first beneficiary
            </button>
          </div>
        ) : (
          <div className="table-responsive">
            <table className="table hfdc-table align-middle mb-0">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Account number</th>
                  <th>Saved on</th>
                  <th className="text-end">Actions</th>
                </tr>
              </thead>
              <tbody>
                {list.map((beneficiary) => (
                  <tr key={beneficiary.beneficiaryId}>
                    <td>
                      <div className="d-flex align-items-center gap-2">
                        <span className="hfdc-avatar hfdc-avatar-soft">
                          {beneficiary.beneficiaryName[0]}
                        </span>
                        <strong>{beneficiary.beneficiaryName}</strong>
                      </div>
                    </td>
                    <td className="hfdc-mono">{beneficiary.beneficiaryAccountNumber}</td>
                    <td className="hfdc-muted-cell">{formatDate(beneficiary.createdAt)}</td>
                    <td className="text-end">
                      <button
                        type="button"
                        className="btn btn-sm btn-outline-danger hfdc-btn"
                        onClick={() => setPendingDelete(beneficiary)}
                      >
                        <Trash2 size={15} /> Remove
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <ConfirmDialog
        open={Boolean(pendingDelete)}
        title="Remove beneficiary?"
        message={
          pendingDelete
            ? `${pendingDelete.beneficiaryName} (${pendingDelete.beneficiaryAccountNumber}) will be removed from your saved list. You can add them again later.`
            : ''
        }
        confirmLabel="Remove"
        busy={deleting}
        onCancel={() => setPendingDelete(null)}
        onConfirm={remove}
      />
    </>
  );
}
