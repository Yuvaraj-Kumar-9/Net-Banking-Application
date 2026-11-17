import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  ArrowDownToLine,
  ArrowUpFromLine,
  Send,
  ReceiptText,
  Wallet,
  TrendingUp,
  TrendingDown,
  RefreshCw,
  AlertCircle,
  ArrowRight,
  Landmark,
} from 'lucide-react';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
} from 'recharts';
import { accountService } from '../services/accountService';
import { apiErrorMessage } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { formatMoney, formatMonth, formatDateTime, transactionLabel } from '../utils/format';
import StatCard from '../components/StatCard';
import Loading from '../components/Loading';
import PageHeader from '../components/PageHeader';

const QUICK_ACTIONS = [
  { to: '/deposit', label: 'Deposit', icon: ArrowDownToLine, tone: 'green' },
  { to: '/withdraw', label: 'Withdraw', icon: ArrowUpFromLine, tone: 'amber' },
  { to: '/transfer', label: 'Transfer', icon: Send, tone: 'blue' },
  { to: '/transactions', label: 'History', icon: ReceiptText, tone: 'navy' },
];

function DirectionBadge({ direction }) {
  const credit = direction === 'CREDIT';
  return (
    <span className={`hfdc-badge ${credit ? 'hfdc-badge-credit' : 'hfdc-badge-debit'}`}>
      {credit ? 'Credit' : 'Debit'}
    </span>
  );
}

/** Live dashboard: every figure comes from GET /api/accounts/dashboard. */
export default function Dashboard() {
  const { user } = useAuth();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setData(await accountService.dashboard());
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const chartData = (data?.monthlySummary || []).map((row) => ({
    month: formatMonth(row.month),
    Deposits: Number(row.deposits || 0),
    Withdrawals: Number(row.withdrawals || 0),
  }));

  const firstName = (data?.fullName || user?.fullName || 'Customer').split(' ')[0];
  const hour = new Date().getHours();
  const greeting = hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening';

  if (loading) return <Loading label="Loading your dashboard…" />;

  if (error) {
    return (
      <div className="hfdc-error-state">
        <AlertCircle size={32} />
        <h2>We could not load your dashboard</h2>
        <p>{error}</p>
        <button type="button" className="btn btn-primary hfdc-btn" onClick={load}>
          <RefreshCw size={16} /> Try again
        </button>
      </div>
    );
  }

  return (
    <>
      <PageHeader
        title={`${greeting}, ${firstName}`}
        subtitle="Here is what is happening with your HFDC Bank account today."
        actions={
          <button type="button" className="btn btn-outline-primary hfdc-btn" onClick={load}>
            <RefreshCw size={16} /> Refresh
          </button>
        }
      />

      <div className="row g-3 g-xl-4">
        <div className="col-sm-6 col-xl-3">
          <StatCard
            icon={Wallet}
            label="Available balance"
            value={formatMoney(data.balance)}
            hint={`${data.accountType} · ${data.accountNumber}`}
            tone="navy"
          />
        </div>
        <div className="col-sm-6 col-xl-3">
          <StatCard
            icon={TrendingUp}
            label="Total credits"
            value={formatMoney(data.totalDeposits)}
            hint="Deposits + incoming transfers"
            tone="green"
          />
        </div>
        <div className="col-sm-6 col-xl-3">
          <StatCard
            icon={TrendingDown}
            label="Total debits"
            value={formatMoney(data.totalWithdrawals)}
            hint="Withdrawals + outgoing transfers"
            tone="amber"
          />
        </div>
        <div className="col-sm-6 col-xl-3">
          <StatCard
            icon={ReceiptText}
            label="Transactions"
            value={String(data.transactionCount)}
            hint="All-time ledger entries"
            tone="blue"
          />
        </div>
      </div>

      <div className="row g-4 mt-1">
        <div className="col-xl-8">
          <div className="hfdc-panel h-100">
            <div className="hfdc-panel-head">
              <div>
                <h3 className="hfdc-panel-title">Cash flow</h3>
                <p className="hfdc-panel-sub">Monthly deposits vs withdrawals (last 12 months)</p>
              </div>
              <span className="hfdc-pill">Live data</span>
            </div>

            {chartData.length === 0 ? (
              <div className="hfdc-empty">
                <TrendingUp size={28} />
                <p>No ledger entries yet. Make a deposit to start the chart.</p>
                <Link to="/deposit" className="btn btn-primary hfdc-btn btn-sm">
                  Deposit money
                </Link>
              </div>
            ) : (
              <div style={{ width: '100%', height: 310 }}>
                <ResponsiveContainer>
                  <BarChart data={chartData} margin={{ top: 10, right: 8, left: -12, bottom: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#e5edf7" vertical={false} />
                    <XAxis dataKey="month" tick={{ fontSize: 12, fill: '#64748b' }} axisLine={false} tickLine={false} />
                    <YAxis
                      tick={{ fontSize: 12, fill: '#64748b' }}
                      axisLine={false}
                      tickLine={false}
                      tickFormatter={(value) => `₹${Math.round(value / 1000)}k`}
                    />
                    <Tooltip
                      formatter={(value) => formatMoney(value)}
                      contentStyle={{ borderRadius: 12, border: '1px solid #e5edf7' }}
                    />
                    <Legend iconType="circle" wrapperStyle={{ fontSize: 13 }} />
                    <Bar dataKey="Deposits" fill="#2563eb" radius={[6, 6, 0, 0]} maxBarSize={26} />
                    <Bar dataKey="Withdrawals" fill="#f59e0b" radius={[6, 6, 0, 0]} maxBarSize={26} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            )}
          </div>
        </div>

        <div className="col-xl-4">
          <div className="hfdc-panel h-100">
            <div className="hfdc-panel-head">
              <h3 className="hfdc-panel-title">Quick actions</h3>
            </div>
            <div className="hfdc-quick-grid">
              {QUICK_ACTIONS.map(({ to, label, icon: Icon, tone }) => (
                <Link key={to} to={to} className={`hfdc-quick-action hfdc-quick-${tone}`}>
                  <span className="hfdc-quick-icon"><Icon size={20} /></span>
                  <span>{label}</span>
                </Link>
              ))}
            </div>

            <div className="hfdc-account-snippet mt-4">
              <div className="d-flex align-items-center gap-2 mb-2">
                <Landmark size={18} />
                <strong>Your account</strong>
              </div>
              <dl>
                <div><dt>Number</dt><dd>{data.accountNumber}</dd></div>
                <div><dt>Type</dt><dd>{data.accountType}</dd></div>
                <div><dt>Status</dt><dd><span className="hfdc-badge hfdc-badge-success">Active</span></dd></div>
              </dl>
              <Link to="/account" className="hfdc-text-link small">
                View account details <ArrowRight size={14} />
              </Link>
            </div>
          </div>
        </div>
      </div>

      <div className="hfdc-panel mt-4">
        <div className="hfdc-panel-head">
          <div>
            <h3 className="hfdc-panel-title">Recent activity</h3>
            <p className="hfdc-panel-sub">Your five most recent ledger entries</p>
          </div>
          <Link to="/transactions" className="btn btn-sm btn-outline-primary hfdc-btn">
            View all <ArrowRight size={14} />
          </Link>
        </div>

        {data.recentTransactions.length === 0 ? (
          <div className="hfdc-empty">
            <ReceiptText size={28} />
            <p>No transactions yet.</p>
          </div>
        ) : (
          <div className="table-responsive">
            <table className="table hfdc-table align-middle mb-0">
              <thead>
                <tr>
                  <th>Reference</th>
                  <th>Type</th>
                  <th>Description</th>
                  <th>Date</th>
                  <th className="text-end">Amount</th>
                </tr>
              </thead>
              <tbody>
                {data.recentTransactions.map((tx) => (
                  <tr key={tx.referenceNumber}>
                    <td className="hfdc-mono">{tx.referenceNumber}</td>
                    <td>{transactionLabel(tx.transactionType)}</td>
                    <td className="hfdc-muted-cell">{tx.description || '—'}</td>
                    <td className="hfdc-muted-cell">{formatDateTime(tx.createdAt)}</td>
                    <td className="text-end">
                      <div className="d-flex flex-column align-items-end gap-1">
                        <strong className={tx.direction === 'CREDIT' ? 'text-success' : 'text-danger'}>
                          {tx.direction === 'CREDIT' ? '+' : '−'}{formatMoney(tx.amount)}
                        </strong>
                        <DirectionBadge direction={tx.direction} />
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </>
  );
}
