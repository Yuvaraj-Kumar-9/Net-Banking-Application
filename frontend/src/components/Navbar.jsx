import { useState } from 'react';
import { Link, NavLink } from 'react-router-dom';
import { Menu, X, ArrowRight, LogIn } from 'lucide-react';
import Logo from './Logo';
import { useAuth } from '../context/AuthContext';

const LINKS = [
  { to: '/', label: 'Home', end: true },
  { to: '/about', label: 'About' },
  { to: '/services', label: 'Services' },
  { to: '/contact', label: 'Contact' },
];

/** Public site navigation bar (responsive, collapses on mobile). */
export default function Navbar() {
  const [open, setOpen] = useState(false);
  const { user } = useAuth();
  const close = () => setOpen(false);

  return (
    <nav className="navbar navbar-expand-lg hfdc-navbar sticky-top">
      <div className="container">
        <Link className="navbar-brand" to="/" onClick={close} aria-label="HFDC Bank home">
          <Logo size={40} />
        </Link>

        <button
          type="button"
          className="navbar-toggler hfdc-navbar-toggler"
          aria-label="Toggle navigation"
          aria-expanded={open}
          onClick={() => setOpen((value) => !value)}
        >
          {open ? <X size={22} /> : <Menu size={22} />}
        </button>

        <div className={`collapse navbar-collapse ${open ? 'show' : ''}`}>
          <ul className="navbar-nav mx-auto gap-lg-1 my-3 my-lg-0">
            {LINKS.map((link) => (
              <li className="nav-item" key={link.to}>
                <NavLink
                  to={link.to}
                  end={link.end}
                  onClick={close}
                  className={({ isActive }) =>
                    `nav-link hfdc-nav-link ${isActive ? 'active' : ''}`
                  }
                >
                  {link.label}
                </NavLink>
              </li>
            ))}
          </ul>

          <div className="d-flex flex-column flex-lg-row align-items-stretch align-items-lg-center gap-2 pb-3 pb-lg-0">
            {user ? (
              <Link to="/dashboard" className="btn btn-primary hfdc-btn" onClick={close}>
                Open dashboard <ArrowRight size={16} />
              </Link>
            ) : (
              <>
                <Link to="/login" className="btn btn-outline-primary hfdc-btn" onClick={close}>
                  <LogIn size={16} /> Sign in
                </Link>
                <Link to="/register" className="btn btn-primary hfdc-btn" onClick={close}>
                  Get started <ArrowRight size={16} />
                </Link>
              </>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
}
