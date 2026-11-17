import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  AlertCircle,
  ArrowDownUp,
  ChevronLeft,
  ChevronRight,
  Filter,
  RefreshCw,
  ReceiptText,
  Search,
  X,
} from 'lucide-react';
import { transactionService } from '../services/transactionService';
import { apiErrorMessage } from '../services/api';
import { formatDateTime, formatMoney, transactionLabel } from '../utils/format';
import PageHeader from '../components/PageHeader';
import Loading from '../components/Loading';

const TYPES = [
  { value: 'ALL', label: 'All types' },
  { value: 'DEPOSIT', label: 'Deposit' },
  { value: 'WITHDRAWAL', label: 'Withdrawal' },
  { value: 'TRANSFER_IN', label: 'Transfer in' },
  { value: 'TRANSFER_OUT', label: 'Transfer out' },
];

const EMPTY_FILTERS = { type: 'ALL', from: '', to: '', search: '' };

/** Searchable, filterable, sortable and paginated transaction history. */
export default function Transactions() {
  const [filters, setFilters] = useState(EMPTY_FILTERS);
  const [searchInput, setSearchInput] = useState('');
  const [sort, setSort] = useState('desc');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [detail, setDetail] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);

  // Debounce free-text search so every keystroke does not hit the API.
  useEffect(() => {
    const timer = window.setTimeout(() => {
      setFilters((current) => ({ ...current, search: searchInput.trim() }));
      setPage(0);
    }, 350);
    return () => window.clearTimeout(timer);
  }, [searchInput]);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const result = await transactionService.list({
        type: filters.type,
        from: filters.from || undefined,
        to: filters.to || undefined,
        search: filters.search || undefined,
        sort,
        page,
        size,
      });
      setData(result);
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [filters, sort, page, size]);

  useEffect(() => {
    load();
  }, [load]);

  const setFilter = (field) => (event) => {
    setFilters((current) => ({ ...current, [field]: event.target.value }));
    setPage(0);
  };

  const reset = () => {
    setFilters(EMPTY_FILTERS);
    setSearchInput('');
    setSort('desc');
    setPage(0);
  };

  const openDetail = async (referenceNumber) => {
    setDetailLoading(true);
    try {
      const result = await transactionService.detail(referenceNumber);
      setDetail(result);
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setDetailLoading(false);
    }
  };

  const pages = useMemo(() => {
    if (!data || data.totalPages <= 0) return [];
    const total = data.totalPages;
    const current = data.page;
    const windowStart = Math.max(0, Math.min(current - 2, total - 5));
    return Array.from({ length: Math.min(5, total) }, (_, index) => windowStart + index);
  }, [data]);

  return (
    <>
      <PageHeader
        title="Transaction history"
        subtitle="Every credit and debit on your account, searchable and filterable."
        actions={
          <button type="button" className="btn btn-outline-primary hfdc-btn" onClick={load}>
            <RefreshCw size={16} /> Refresh
          </button>
        }
      />

      <div className="hfdc-panel">
        <div className="hfdc-filter-bar">
          <div className="hfdc-filter-search">
            <Search size={17} />
            <input
              type="search"
              className="form-control"
              placeholder="Search reference, description or type…"
              value={searchInput}
              onChange={(event) => setSearchInput(event.target.value)}
              aria-label="Search transactions"
            />
          </div>

          <select className="form-select" value={filters.type} onChange={setFilter('type')} aria-label="Filter by type">
            {TYPES.map((type) => (
              <option key={type.value} value={type.value}>{type.label}</option>
            ))}
          </select>

          <input
            type="date"
            className="form-control"
            value={filters.from}
            onChange={setFilter('from')}
            aria-label="From date"
          />
          <input
            type="date"
            className="form-control"
            value={filters.to}
            onChange={setFilter('to')}
            aria-label="To date"
          />

          <button
            type="button"
            className="btn btn-outline-primary hfdc-btn"
            onClick={() => setSort((current) => (current === 'desc' ? 'asc' : 'desc'))}
            title="Toggle sort direction"
          >
            <ArrowDownUp size={16} /> {sort === 'desc' ? 'Newest first' : 'Oldest first'}
          </button>

          <button type="button" className="btn btn-light hfdc-btn" onClick={reset}>
            <X size={16} /> Reset
          </button>
        </div>

        {error && (
          <div className="alert alert-danger d-flex align-items-start gap-2 mx-3 mt-3" role="alert">
            <AlertCircle size={18} className="flex-shrink-0 mt-1" />
            <span>{error}</span>
          </div>
        )}

        {loading ? (
          <Loading label="Querying your ledger…" />
        ) : !data || data.content.length === 0 ? (
          <div className="hfdc-empty">
            <Filter size={30} />
            <h3>No matching transactions</h3>
            <p>Try clearing the search box, widening the date range or choosing another type.</p>
            <button type="button" className="btn btn-primary hfdc-btn btn-sm" onClick={reset}>
              Clear filters
            </button>
          </div>
        ) : (
          <>
            <div className="table-responsive">
              <table className="table hfdc-table align-middle mb-0">
                <thead>
                  <tr>
                    <th>Reference</th>
                    <th>Type</th>
                    <th>Description</th>
                    <th>Date &amp; time</th>
                    <th>Status</th>
                    <th className="text-end">Amount</th>
                    <th className="text-end">Details</th>
                  </tr>
                </thead>
                <tbody>
                  {data.content.map((tx) => (
                    <tr key={`${tx.referenceNumber}-${tx.transactionId}`}>
                      <td className="hfdc-mono">{tx.referenceNumber}</td>
                      <td>
                        <span className={`hfdc-badge ${tx.direction === 'CREDIT' ? 'hfdc-badge-credit' : 'hfdc-badge-debit'}`}>
                          {transactionLabel(tx.transactionType)}
                        </span>
                      </td>
                      <td className="hfdc-muted-cell">{tx.description || '—'}</td>
                      <td className="hfdc-muted-cell">{formatDateTime(tx.createdAt)}</td>
                      <td>
                        <span className="hfdc-badge hfdc-badge-success">{tx.transactionStatus}</span>
                      </td>
                      <td className={`text-end fw-semibold ${tx.direction === 'CREDIT' ? 'text-success' : 'text-danger'}`}>
                        {tx.direction === 'CREDIT' ? '+' : '−'}{formatMoney(tx.amount)}
                      </td>
                      <td className="text-end">
                        <button
                          type="button"
                          className="btn btn-sm btn-outline-primary hfdc-btn"
                          onClick={() => openDetail(tx.referenceNumber)}
                        >
                          View
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="hfdc-table-foot">
              <div className="d-flex align-items-center gap-2">
                <span className="text-muted small">
                  Showing {data.page * data.size + 1}–
                  {Math.min((data.page + 1) * data.size, data.totalElements)} of {data.totalElements}
                </span>
                <select
                  className="form-select form-select-sm hfdc-page-size"
                  value={size}
                  onChange={(event) => {
                    setSize(Number(event.target.value));
                    setPage(0);
                  }}
                  aria-label="Rows per page"
                >
                  {[5, 10, 20, 50].map((option) => (
                    <option key={option} value={option}>{option} / page</option>
                  ))}
                </select>
              </div>

              <nav className="hfdc-pagination" aria-label="Transaction pages">
                <button
                  type="button"
                  className="btn btn-sm btn-light hfdc-btn"
                  onClick={() => setPage((current) => Math.max(0, current - 1))}
                  disabled={data.first}
                  aria-label="Previous page"
                >
                  <ChevronLeft size={16} />
                </button>
                {pages.map((pageNumber) => (
                  <button
                    key={pageNumber}
                    type="button"
                    className={`btn btn-sm hfdc-btn ${pageNumber === data.page ? 'btn-primary' : 'btn-light'}`}
                    onClick={() => setPage(pageNumber)}
                  >
                    {pageNumber + 1}
                  </button>
                ))}
                <button
                  type="button"
                  className="btn btn-sm btn-light hfdc-btn"
                  onClick={() => setPage((current) => Math.min(data.totalPages - 1, current + 1))}
                  disabled={data.last}
                  aria-label="Next page"
                >
                  <ChevronRight size={16} />
                </button>
              </nav>
            </div>
          </>
        )}
      </div>

      {(detail || detailLoading) && (
        <div className="hfdc-modal-backdrop" role="presentation" onClick={() => setDetail(null)}>
          <div
            className="hfdc-modal hfdc-modal-wide"
            role="dialog"
            aria-modal="true"
            aria-labelledby="tx-detail-title"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="d-flex justify-content-between align-items-start">
              <div>
                <span className="hfdc-eyebrow">
                  <ReceiptText size={14} /> Ledger entry
                </span>
                <h3 id="tx-detail-title">Transaction details</h3>
              </div>
              <button
                type="button"
                className="btn btn-sm btn-light hfdc-btn"
                onClick={() => setDetail(null)}
                aria-label="Close details"
              >
                <X size={16} />
              </button>
            </div>

            {detailLoading ? (
              <Loading label="Loading transaction…" />
            ) : (
              <dl className="hfdc-receipt mt-3">
                <div><dt>Reference</dt><dd className="hfdc-mono">{detail.referenceNumber}</dd></div>
                <div><dt>Transaction ID</dt><dd>#{detail.transactionId}</dd></div>
                <div><dt>Type</dt><dd>{transactionLabel(detail.transactionType)}</dd></div>
                <div><dt>Direction</dt><dd>{detail.direction}</dd></div>
                <div>
                  <dt>Amount</dt>
                  <dd className={detail.direction === 'CREDIT' ? 'text-success' : 'text-danger'}>
                    {formatMoney(detail.amount)}
                  </dd>
                </div>
                <div><dt>Status</dt><dd>{detail.transactionStatus}</dd></div>
                <div><dt>Description</dt><dd>{detail.description || '—'}</dd></div>
                <div><dt>Recorded at</dt><dd>{formatDateTime(detail.createdAt)}</dd></div>
              </dl>
            )}
          </div>
        </div>
      )}
    </>
  );
}
