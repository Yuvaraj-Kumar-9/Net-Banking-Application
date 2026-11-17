import { useEffect, useState } from 'react';
import { Link, Outlet, useLocation } from 'react-router-dom';
import { Menu, Bell, ShieldCheck } from 'lucide-react';
import Sidebar from './Sidebar';
import { useAuth } from '../context/AuthContext';
import { initials } from '../utils/format';
import Disclaimer from './Disclaimer';

const TITLES = {
  '/dashboard': ['Dashboard', 'A live snapshot of your HFDC Bank account'],
  '/account': ['My Account', 'Your customer and account information'],
  '/balance': ['Balance Enquiry', 'Available balance, refreshed from MySQL'],
  '/deposit': ['Deposit Money', 'Add simulated funds to your account'],
  '/withdraw': ['Withdraw Money', 'Secure withdrawal with your transaction PIN'],
  '/transfer': ['Fund Transfer', 'Atomic, PIN-protected peer-to-peer transfer'],
  '/transactions': ['Transaction History', 'Search, filter and review every entry'],
  '/beneficiaries': ['Beneficiaries', 'Manage the accounts you transfer to'],
  '/profile': ['Profile', 'Keep your contact details up to date'],
  '/change-password': ['Change Password', 'Update your login credentials securely'],
};

/** Shell for all authenticated pages: sidebar + top bar + content. */
export default function AppLayout() {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const { user } = useAuth();
  const location = useLocation();
  const [title, subtitle] = TITLES[location.pathname] || ['HFDC Bank', 'Secure net banking'];

  useEffect(() => {
    setSidebarOpen(false);
  }, [location.pathname]);

  return (
    <div className="hfdc-app-shell">
      <Sidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />

      <div className="hfdc-app-main">
        <header className="hfdc-topbar">
          <button
            type="button"
            className="btn hfdc-icon-btn d-lg-none"
            onClick={() => setSidebarOpen(true)}
            aria-label="Open navigation"
          >
            <Menu size={22} />
          </button>

          <div className="hfdc-topbar-title">
            <h1>{title}</h1>
            <p>{subtitle}</p>
          </div>

          <div className="ms-auto d-flex align-items-center gap-2 gap-sm-3">
            <span className="badge hfdc-demo-badge d-none d-sm-inline-flex">
              <ShieldCheck size={14} /> Demo mode
            </span>
            <button type="button" className="btn hfdc-icon-btn position-relative" aria-label="Notifications">
              <Bell size={20} />
              <span className="hfdc-notification-dot" />
            </button>
            <Link to="/profile" className="hfdc-avatar" title="Open profile">
              {initials(user?.fullName)}
            </Link>
          </div>
        </header>

        <main className="hfdc-content">
          <Outlet />
          <div className="mt-4">
            <Disclaimer compact />
          </div>
        </main>

        <footer className="hfdc-app-footer">
          <span>© {new Date().getFullYear()} HFDC Bank</span>
          <span className="d-none d-sm-inline">Educational simulation · No real money</span>
        </footer>
      </div>
    </div>
  );
}
