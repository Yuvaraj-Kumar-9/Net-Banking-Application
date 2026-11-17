import { useState } from 'react';
import { Link } from 'react-router-dom';
import {
  ArrowRight,
  ShieldCheck,
  Zap,
  Lock,
  Landmark,
  Wallet,
  Send,
  ReceiptText,
  Users,
  Smartphone,
  BadgeCheck,
  Headphones,
  ChevronDown,
  TrendingUp,
  CircleDollarSign,
  FileCheck2,
  Sparkles,
} from 'lucide-react';
import HeroIllustration from '../components/HeroIllustration';
import Disclaimer from '../components/Disclaimer';

const SERVICES = [
  {
    icon: Wallet,
    title: 'Instant deposits',
    text: 'Add simulated funds in seconds and watch your ledger update in real time.',
  },
  {
    icon: Send,
    title: 'Atomic fund transfers',
    text: 'PIN-protected peer transfers that either complete fully or not at all.',
  },
  {
    icon: ReceiptText,
    title: 'Smart transaction history',
    text: 'Search, filter, sort and open any entry with its full reference details.',
  },
  {
    icon: Users,
    title: 'Beneficiary management',
    text: 'Save verified recipients once, then pay them in two clicks.',
  },
  {
    icon: Landmark,
    title: 'Account insights',
    text: 'A live dashboard with balances, monthly flows and recent activity.',
  },
  {
    icon: ShieldCheck,
    title: 'Security by design',
    text: 'BCrypt hashing, HttpOnly JWT cookies and CSRF protection on every action.',
  },
];

const REASONS = [
  {
    icon: Zap,
    title: 'Built for speed',
    text: 'A React single-page app talking to a lean Spring Boot API. No page reloads, no waiting.',
  },
  {
    icon: FileCheck2,
    title: 'Clean, layered code',
    text: 'Controllers, services and JDBC repositories are separated so every layer stays testable.',
  },
  {
    icon: BadgeCheck,
    title: 'Correct by construction',
    text: 'Money is stored as DECIMAL(19,2) and moved inside real database transactions.',
  },
  {
    icon: Smartphone,
    title: 'Responsive everywhere',
    text: 'The same premium experience on a phone, tablet or widescreen desktop.',
  },
];

const SECURITY_POINTS = [
  { icon: Lock, title: 'HttpOnly JWT cookies', text: 'The token is never readable by JavaScript or stored in localStorage.' },
  { icon: ShieldCheck, title: 'BCrypt password + PIN hashing', text: 'Slow, salted hashes protect both your login and your transaction PIN.' },
  { icon: FileCheck2, title: 'Double-submit CSRF tokens', text: 'Every state-changing request must prove it came from the app itself.' },
  { icon: Landmark, title: 'Row-level locking', text: 'Transfers lock accounts in a fixed order, preventing double-spend and deadlocks.' },
];

const TESTIMONIALS = [
  {
    name: 'Ananya R.',
    role: 'Illustrative persona · Product designer',
    quote:
      'The dashboard feels like a real bank app. I can see exactly where my simulated money went.',
  },
  {
    name: 'Rohit M.',
    role: 'Illustrative persona · CS student',
    quote:
      'Reading the transfer code taught me more about database transactions than a whole semester.',
  },
  {
    name: 'Meera K.',
    role: 'Illustrative persona · Small business owner',
    quote:
      'Adding a beneficiary and paying them took under a minute. The PIN prompt is a nice touch.',
  },
];

const FAQS = [
  {
    q: 'Is HFDC Bank a real bank?',
    a: 'No. HFDC Bank is a fictional educational banking simulation. It is not a real financial institution and no real money, payments or financial services are involved.',
  },
  {
    q: 'Can I use real money here?',
    a: 'Never. All deposits, withdrawals and transfers are simulated entries in a local MySQL database. There is no connection to any payment network.',
  },
  {
    q: 'How do I try the demo?',
    a: 'Sign in with aarav@hfdcbank.test / Demo@1234 (transaction PIN 1234) or create your own account from the registration page.',
  },
  {
    q: 'What happens to the data I enter?',
    a: 'It stays in the development database for this project. Passwords and transaction PINs are stored only as BCrypt hashes.',
  },
  {
    q: 'Which technologies power it?',
    a: 'React, React Router, Axios and Bootstrap 5 on the frontend; Java 21, Spring Boot, Spring Security, Spring JDBC and MySQL 8 on the backend.',
  },
];

function Faq() {
  const [openIndex, setOpenIndex] = useState(0);
  return (
    <div className="hfdc-faq">
      {FAQS.map((item, index) => {
        const open = openIndex === index;
        return (
          <div className={`hfdc-faq-item ${open ? 'open' : ''}`} key={item.q}>
            <button
              type="button"
              className="hfdc-faq-question"
              aria-expanded={open}
              onClick={() => setOpenIndex(open ? -1 : index)}
            >
              <span>{item.q}</span>
              <ChevronDown size={20} className="hfdc-faq-chevron" />
            </button>
            {open && <div className="hfdc-faq-answer">{item.a}</div>}
          </div>
        );
      })}
    </div>
  );
}

/** Public landing page: hero, services, reasons, security, testimonials, FAQ. */
export default function Home() {
  return (
    <>
      {/* ------------------------------------------------------------- Hero */}
      <section className="hfdc-hero">
        <div className="container">
          <div className="row align-items-center g-5">
            <div className="col-lg-6">
              <span className="hfdc-eyebrow">
                <Sparkles size={15} /> A modern banking simulation
              </span>
              <h1 className="hfdc-hero-title">
                Banking <span className="text-gradient">Beyond Boundaries</span>
              </h1>
              <p className="hfdc-hero-sub">
                Experience net banking the way it should feel: instant balances, transparent
                transactions and transfers that are safe down to the last paisa. Built as a
                full-stack engineering showcase.
              </p>
              <div className="d-flex flex-column flex-sm-row gap-3 mt-4">
                <Link to="/register" className="btn btn-primary btn-lg hfdc-btn">
                  Open a demo account <ArrowRight size={18} />
                </Link>
                <Link to="/login" className="btn btn-outline-primary btn-lg hfdc-btn">
                  Sign in to demo
                </Link>
              </div>

              <div className="hfdc-hero-stats">
                <div>
                  <strong>100%</strong>
                  <span>simulated funds</span>
                </div>
                <div>
                  <strong>12</strong>
                  <span>months of insights</span>
                </div>
                <div>
                  <strong>0</strong>
                  <span>real transactions</span>
                </div>
              </div>
            </div>
            <div className="col-lg-6">
              <HeroIllustration />
            </div>
          </div>
        </div>
      </section>

      {/* -------------------------------------------------------- Trust strip */}
      <section className="hfdc-trust-strip">
        <div className="container">
          <div className="row g-4 text-center text-md-start">
            {[
              { icon: ShieldCheck, label: 'JWT + CSRF protected' },
              { icon: TrendingUp, label: 'Live MySQL figures' },
              { icon: Lock, label: 'BCrypt hashing' },
              { icon: CircleDollarSign, label: 'DECIMAL money math' },
            ].map(({ icon: Icon, label }) => (
              <div className="col-6 col-md-3" key={label}>
                <div className="hfdc-trust-item">
                  <Icon size={20} />
                  <span>{label}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ------------------------------------------------------------ Services */}
      <section className="hfdc-section" id="services">
        <div className="container">
          <div className="hfdc-section-head">
            <span className="hfdc-eyebrow">What you can do</span>
            <h2>Everything you need to run your money</h2>
            <p>
              Six focused features, each backed by real API calls and database records - nothing
              on the dashboard is faked in the browser.
            </p>
          </div>

          <div className="row g-4">
            {SERVICES.map(({ icon: Icon, title, text }) => (
              <div className="col-md-6 col-lg-4" key={title}>
                <article className="hfdc-feature-card">
                  <div className="hfdc-feature-icon">
                    <Icon size={22} />
                  </div>
                  <h3>{title}</h3>
                  <p>{text}</p>
                </article>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ------------------------------------------------------- Why choose us */}
      <section className="hfdc-section hfdc-section-alt">
        <div className="container">
          <div className="row g-5 align-items-center">
            <div className="col-lg-5">
              <span className="hfdc-eyebrow">Why HFDC Bank</span>
              <h2 className="hfdc-section-title">Engineered like a real bank, explained like a course</h2>
              <p className="text-muted mb-4">
                HFDC Bank is a portfolio project that takes banking rules seriously. It is also a
                readable reference for anyone learning how a modern full-stack app fits together.
              </p>
              <Link to="/about" className="btn btn-outline-primary hfdc-btn">
                Read our story <ArrowRight size={16} />
              </Link>
            </div>
            <div className="col-lg-7">
              <div className="row g-4">
                {REASONS.map(({ icon: Icon, title, text }) => (
                  <div className="col-sm-6" key={title}>
                    <div className="hfdc-reason-card">
                      <Icon size={22} className="hfdc-reason-icon" />
                      <h3>{title}</h3>
                      <p>{text}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ------------------------------------------------------------ Security */}
      <section className="hfdc-section hfdc-security-section" id="security">
        <div className="container">
          <div className="hfdc-section-head">
            <span className="hfdc-eyebrow hfdc-eyebrow-light">Security</span>
            <h2>Your session is defended at every layer</h2>
            <p>
              The same patterns used by production banking systems, implemented in a codebase you
              can read line by line.
            </p>
          </div>
          <div className="row g-4">
            {SECURITY_POINTS.map(({ icon: Icon, title, text }) => (
              <div className="col-md-6 col-lg-3" key={title}>
                <div className="hfdc-security-card">
                  <div className="hfdc-security-icon">
                    <Icon size={22} />
                  </div>
                  <h3>{title}</h3>
                  <p>{text}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* --------------------------------------------------------- Testimonials */}
      <section className="hfdc-section">
        <div className="container">
          <div className="hfdc-section-head">
            <span className="hfdc-eyebrow">Illustrative testimonials</span>
            <h2>What a demo user might say</h2>
            <p className="fst-italic">
              These quotes are illustrative only. They were written for this educational project
              and do not represent real customers or a real bank.
            </p>
          </div>
          <div className="row g-4">
            {TESTIMONIALS.map((item) => (
              <div className="col-md-4" key={item.name}>
                <figure className="hfdc-testimonial-card">
                  <blockquote>“{item.quote}”</blockquote>
                  <figcaption>
                    <span className="hfdc-avatar hfdc-avatar-soft">{item.name[0]}</span>
                    <div>
                      <strong>{item.name}</strong>
                      <span>{item.role}</span>
                    </div>
                  </figcaption>
                </figure>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ------------------------------------------------------------- FAQ */}
      <section className="hfdc-section hfdc-section-alt" id="faq">
        <div className="container">
          <div className="row g-5">
            <div className="col-lg-4">
              <span className="hfdc-eyebrow">Questions</span>
              <h2 className="hfdc-section-title">Good to know</h2>
              <p className="text-muted">
                Still curious? The About and Services pages explain the architecture and the
                feature set in more detail.
              </p>
              <Link to="/contact" className="btn btn-primary hfdc-btn mt-2">
                <Headphones size={16} /> Contact the project
              </Link>
            </div>
            <div className="col-lg-8">
              <Faq />
            </div>
          </div>
        </div>
      </section>

      {/* ------------------------------------------------------------- CTA */}
      <section className="hfdc-cta">
        <div className="container">
          <div className="hfdc-cta-inner">
            <div>
              <h2>Ready to explore the dashboard?</h2>
              <p>Create an account in under a minute, or use the seeded demo login.</p>
            </div>
            <div className="d-flex flex-column flex-sm-row gap-3">
              <Link to="/register" className="btn btn-light btn-lg hfdc-btn">
                Create account <ArrowRight size={18} />
              </Link>
              <Link to="/login" className="btn btn-outline-light btn-lg hfdc-btn">
                Demo login
              </Link>
            </div>
          </div>
          <div className="mt-4">
            <Disclaimer />
          </div>
        </div>
      </section>
    </>
  );
}
