import { Link } from 'react-router-dom';
import { Mail, MapPin, Phone } from 'lucide-react';
import Logo from './Logo';
import Disclaimer from './Disclaimer';

/** Public site footer with the fictional-bank disclaimer. */
export default function Footer() {
  return (
    <footer className="hfdc-footer">
      <div className="container py-5">
        <div className="row g-4">
          <div className="col-lg-4">
            <Logo size={44} light />
            <p className="text-white-50 mt-3 mb-3" style={{ maxWidth: 320 }}>
              Banking Beyond Boundaries. A modern, educational net-banking experience built with
              React, Spring Boot and MySQL.
            </p>
            <Disclaimer compact />
          </div>

          <div className="col-6 col-lg-2">
            <h6 className="hfdc-footer-title">Bank</h6>
            <ul className="list-unstyled hfdc-footer-links">
              <li><Link to="/">Home</Link></li>
              <li><Link to="/about">About us</Link></li>
              <li><Link to="/services">Services</Link></li>
              <li><Link to="/contact">Contact</Link></li>
            </ul>
          </div>

          <div className="col-6 col-lg-2">
            <h6 className="hfdc-footer-title">Account</h6>
            <ul className="list-unstyled hfdc-footer-links">
              <li><Link to="/login">Sign in</Link></li>
              <li><Link to="/register">Open an account</Link></li>
              <li><Link to="/forgot-password">Forgot password</Link></li>
              <li><Link to="/dashboard">Dashboard</Link></li>
            </ul>
          </div>

          <div className="col-lg-4">
            <h6 className="hfdc-footer-title">Demo support</h6>
            <ul className="list-unstyled hfdc-footer-links">
              <li><Mail size={16} /> support@hfdcbank.test</li>
              <li><Phone size={16} /> +91 00000 00000</li>
              <li><MapPin size={16} /> 1 Demo Avenue, Mumbai, India</li>
            </ul>
            <p className="text-white-50 small mb-0">
              No real money, payments or financial services are involved.
            </p>
          </div>
        </div>

        <hr className="border-secondary my-4" />
        <div className="d-flex flex-column flex-md-row justify-content-between gap-2 text-white-50 small">
          <span>© {new Date().getFullYear()} HFDC Bank (educational simulation).</span>
          <span>Built for learning · Not affiliated with any real bank.</span>
        </div>
      </div>
    </footer>
  );
}
