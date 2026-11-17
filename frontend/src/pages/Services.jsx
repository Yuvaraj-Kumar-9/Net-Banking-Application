import { Link } from 'react-router-dom';
import {
  ArrowRight,
  Wallet,
  Send,
  ReceiptText,
  Users,
  Landmark,
  ShieldCheck,
  Search,
  Filter,
  KeyRound,
  Gauge,
  CheckCircle2,
} from 'lucide-react';
import Disclaimer from '../components/Disclaimer';

const FEATURES = [
  {
    icon: Wallet,
    title: 'Balance enquiry',
    text: 'Fetch the live available balance plus the timestamp of the most recent ledger entry.',
    points: ['Real MySQL balance', 'Last-updated timestamp', 'One-click refresh'],
    to: '/balance',
  },
  {
    icon: Wallet,
    title: 'Simulated deposits',
    text: 'Add demo funds with a description. The balance update and ledger row share one transaction.',
    points: ['Up to ₹10,00,000 per entry', 'Instant ledger reference', 'No real payment rails'],
    to: '/deposit',
  },
  {
    icon: KeyRound,
    title: 'PIN-protected withdrawals',
    text: 'Withdrawals verify your BCrypt-hashed transaction PIN before any money moves.',
    points: ['4-6 digit PIN', 'Insufficient-balance guard', 'Atomic debit + ledger write'],
    to: '/withdraw',
  },
  {
    icon: Send,
    title: 'Atomic fund transfers',
    text: 'Transfers lock both account rows, debit, credit and write two ledger rows in one transaction.',
    points: ['Deadlock-safe lock ordering', 'Saved beneficiaries', 'Reference confirmation'],
    to: '/transfer',
  },
  {
    icon: ReceiptText,
    title: 'Transaction history',
    text: 'Search by reference or description, filter by type and date, then sort and paginate.',
    points: ['Server-side filtering', 'Pagination', 'Detail view per reference'],
    to: '/transactions',
  },
  {
    icon: Users,
    title: 'Beneficiaries',
    text: 'Save verified HFDC accounts and reuse them for transfers without retyping numbers.',
    points: ['Search saved payees', 'Duplicate protection', 'Owner-scoped deletes'],
    to: '/beneficiaries',
  },
];

const EXTRAS = [
  { icon: Landmark, title: 'Account profile', text: 'Account number, type, status and opening date.' },
  { icon: Gauge, title: 'Dashboard analytics', text: 'Monthly deposits vs withdrawals for the last 12 months.' },
  { icon: ShieldCheck, title: 'Security settings', text: 'Change your password after verifying the current one.' },
  { icon: Search, title: 'Smart search', text: 'Match references, descriptions and transaction types.' },
  { icon: Filter, title: 'Flexible filters', text: 'Combine type, date range and free-text search.' },
  { icon: KeyRound, title: 'Password reset', text: 'Short-lived, single-use reset tokens (dev mode shows the token).' },
];

/** Marketing page listing every implemented banking feature. */
export default function Services() {
  return (
    <>
      <section className="hfdc-page-hero">
        <div className="container">
          <span className="hfdc-eyebrow">Services</span>
          <h1>Six features, zero shortcuts</h1>
          <p>
            Each service below is backed by a protected API endpoint, server-side validation and a
            real database write. Click any card to jump straight to the live page after signing in.
          </p>
          <Disclaimer />
        </div>
      </section>

      <section className="hfdc-section">
        <div className="container">
          <div className="row g-4">
            {FEATURES.map(({ icon: Icon, title, text, points, to }) => (
              <div className="col-md-6 col-xl-4" key={title}>
                <article className="hfdc-service-card">
                  <div className="hfdc-feature-icon">
                    <Icon size={22} />
                  </div>
                  <h3>{title}</h3>
                  <p>{text}</p>
                  <ul className="hfdc-check-list hfdc-check-list-sm">
                    {points.map((point) => (
                      <li key={point}>
                        <CheckCircle2 size={15} /> {point}
                      </li>
                    ))}
                  </ul>
                  <Link to={to} className="hfdc-text-link">
                    Open page <ArrowRight size={15} />
                  </Link>
                </article>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="hfdc-section hfdc-section-alt">
        <div className="container">
          <div className="hfdc-section-head">
            <span className="hfdc-eyebrow">Also included</span>
            <h2>The details that make it feel complete</h2>
          </div>
          <div className="row g-4">
            {EXTRAS.map(({ icon: Icon, title, text }) => (
              <div className="col-sm-6 col-lg-4" key={title}>
                <div className="hfdc-mini-card">
                  <Icon size={20} />
                  <div>
                    <h3>{title}</h3>
                    <p>{text}</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="hfdc-cta">
        <div className="container">
          <div className="hfdc-cta-inner">
            <div>
              <h2>Try every service with demo data</h2>
              <p>Two seeded customers and a full transaction history are waiting.</p>
            </div>
            <Link to="/login" className="btn btn-light btn-lg hfdc-btn">
              Sign in <ArrowRight size={18} />
            </Link>
          </div>
        </div>
      </section>
    </>
  );
}
