import { Link } from 'react-router-dom';
import {
  ArrowRight,
  Building2,
  Database,
  Layers,
  ShieldCheck,
  Target,
  Users,
  Code2,
  BookOpen,
} from 'lucide-react';
import Disclaimer from '../components/Disclaimer';

const VALUES = [
  {
    icon: ShieldCheck,
    title: 'Safety before features',
    text: 'Every money movement is transactional, validated on the server and protected by authentication.',
  },
  {
    icon: BookOpen,
    title: 'Clarity over cleverness',
    text: 'Readable layers, named SQL, and comments that explain the why - not just the what.',
  },
  {
    icon: Users,
    title: 'Respect for the user',
    text: 'Clear error messages, accessible forms, honest copy and a visible disclaimer everywhere.',
  },
];

const STACK = [
  { label: 'Frontend', value: 'React 18, React Router, Axios, Bootstrap 5, Recharts' },
  { label: 'Backend', value: 'Java 21, Spring Boot 3, Spring Security, Spring JDBC' },
  { label: 'Database', value: 'MySQL 8 with DECIMAL(19,2) money columns' },
  { label: 'Security', value: 'JWT in HttpOnly cookies, BCrypt hashing, CSRF double-submit' },
];

/** Static story page describing the project and its engineering choices. */
export default function About() {
  return (
    <>
      <section className="hfdc-page-hero">
        <div className="container">
          <span className="hfdc-eyebrow">About the project</span>
          <h1>A bank built to be understood</h1>
          <p>
            HFDC Bank is a fictional educational banking simulation. It exists to demonstrate how
            a modern full-stack banking experience is designed, secured and tested - not to handle
            anyone&apos;s real money.
          </p>
          <Disclaimer />
        </div>
      </section>

      <section className="hfdc-section">
        <div className="container">
          <div className="row g-5 align-items-start">
            <div className="col-lg-6">
              <h2 className="hfdc-section-title">Why we built it</h2>
              <p className="text-muted">
                Most banking demos stop at a pretty dashboard. HFDC Bank goes further: the balance
                you see is a MySQL row, the chart is an aggregate query, and a transfer either
                debits and credits both accounts or changes nothing at all.
              </p>
              <p className="text-muted">
                The project doubles as a learning reference. The codebase is split into
                controllers, services, repositories and DTOs, with no JPA magic hiding the SQL.
                Every table, index and constraint is in a plain schema file you can read.
              </p>
              <div className="hfdc-value-grid">
                {VALUES.map(({ icon: Icon, title, text }) => (
                  <div className="hfdc-value-card" key={title}>
                    <Icon size={20} />
                    <h3>{title}</h3>
                    <p>{text}</p>
                  </div>
                ))}
              </div>
            </div>

            <div className="col-lg-6">
              <div className="hfdc-panel">
                <h3 className="hfdc-panel-title">
                  <Layers size={20} /> Technology stack
                </h3>
                <dl className="hfdc-stack-list">
                  {STACK.map((item) => (
                    <div key={item.label}>
                      <dt>{item.label}</dt>
                      <dd>{item.value}</dd>
                    </div>
                  ))}
                </dl>
              </div>

              <div className="hfdc-panel mt-4">
                <h3 className="hfdc-panel-title">
                  <Target size={20} /> Project goals
                </h3>
                <ul className="hfdc-check-list">
                  <li>Demonstrate atomic money movement with row locking</li>
                  <li>Show secure session handling without localStorage tokens</li>
                  <li>Practise clean layered architecture with Spring JDBC</li>
                  <li>Deliver a polished, responsive and accessible UI</li>
                  <li>Document setup, testing and deployment end to end</li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="hfdc-section hfdc-section-alt">
        <div className="container">
          <div className="hfdc-section-head">
            <span className="hfdc-eyebrow">Architecture at a glance</span>
            <h2>Three clean layers, one honest flow</h2>
          </div>
          <div className="row g-4">
            {[
              {
                icon: Code2,
                step: '01',
                title: 'React client',
                text: 'Pages call typed service modules. Axios attaches the CSRF header and sends the HttpOnly cookie automatically.',
              },
              {
                icon: Building2,
                step: '02',
                title: 'Spring Boot API',
                text: 'Controllers validate input, services enforce banking rules, and repositories issue explicit, parameterised SQL.',
              },
              {
                icon: Database,
                step: '03',
                title: 'MySQL 8',
                text: 'Constraints and transactions are the final authority. Balances are DECIMAL(19,2) and never floating point.',
              },
            ].map(({ icon: Icon, step, title, text }) => (
              <div className="col-md-4" key={step}>
                <div className="hfdc-arch-card">
                  <span className="hfdc-arch-step">{step}</span>
                  <Icon size={24} />
                  <h3>{title}</h3>
                  <p>{text}</p>
                </div>
              </div>
            ))}
          </div>
          <div className="text-center mt-5">
            <Link to="/services" className="btn btn-primary hfdc-btn">
              Explore the services <ArrowRight size={16} />
            </Link>
          </div>
        </div>
      </section>

      <section className="hfdc-cta">
        <div className="container">
          <div className="hfdc-cta-inner">
            <div>
              <h2>See the architecture in action</h2>
              <p>Sign in and make a simulated transfer to watch every layer work together.</p>
            </div>
            <Link to="/login" className="btn btn-light btn-lg hfdc-btn">
              Open the demo <ArrowRight size={18} />
            </Link>
          </div>
        </div>
      </section>
    </>
  );
}
