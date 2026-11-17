import { NavLink, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard,
  Wallet,
  Landmark,
  ArrowDownToLine,
  ArrowUpFromLine,
  Send,
  ReceiptText,
  Users,
  UserRound,
  KeyRound,
  LogOut,
  X,
  ShieldCheck,
} from 'lucide-react';
import Logo from './Logo';
import { useAuth } from '../context/AuthContext';
import { useNotify } from '../context/NotificationContext';
import { initials } from '../utils/format';

const NAV_ITEMS = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/account', label: 'My Account', icon: Landmark },
  { to: '/balance', label: 'Balance Enquiry', icon: Wallet },
  { to: '/deposit', label: 'Deposit Money', icon: ArrowDownToLine },
  { to: '/withdraw', label: 'Withdraw Money', icon: ArrowUpFromLine },
  { to: '/transfer', label: 'Fund Transfer', icon: Send },
  { to: '/transactions', label: 'Transaction History', icon: ReceiptText },
  { to: '/beneficiaries', label: 'Beneficiaries', icon: Users },
  { to: '/profile', label: 'Profile', icon: UserRound },
  { to: '/change-password', label: 'Change Password', icon: KeyRound },
];

/** Dashboard sidebar. On mobile it slides over the content. */
export default function Sidebar({ open, onClose }) {
  const { user, logout } = useAuth();
  const { notify } = useNotify();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    notify('You have been signed out safely.', 'info');
    navigate('/login', { replace: true });
  };

  return (
    <>
      <div
        className={`hfdc-sidebar-backdrop ${open ? 'show' : ''}`}
        onClick={onClose}
        aria-hidden="true"
      />
      <aside className={`hfdc-sidebar ${open ? 'open' : ''}`} aria-label="Dashboard navigation">
        <div className="hfdc-sidebar-head">
          <NavLink to="/dashboard" onClick={onClose} className="text-decoration-none">
            <Logo size={38} light />
          </NavLink>
          <button
            type="button"
            className="btn btn-sm hfdc-sidebar-close d-lg-none"
            onClick={onClose}
            aria-label="Close navigation"
          >
            <X size={20} />
          </button>
        </div>

        <div className="hfdc-sidebar-user">
          <span className="hfdc-avatar hfdc-avatar-light">{initials(user?.fullName)}</span>
          <div className="overflow-hidden">
            <div className="fw-semibold text-truncate">{user?.fullName || 'HFDC Customer'}</div>
            <div className="small text-white-50 text-truncate">{user?.customerId}</div>
          </div>
        </div>

        <nav className="hfdc-sidebar-nav">
          {NAV_ITEMS.map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              onClick={onClose}
              className={({ isActive }) => `hfdc-side-link ${isActive ? 'active' : ''}`}
            >
              <Icon size={18} />
              <span>{label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="hfdc-sidebar-foot">
          <div className="hfdc-sidebar-secure">
            <ShieldCheck size={16} />
            <span>Session secured with JWT + CSRF</span>
          </div>
          <button type="button" className="hfdc-side-link hfdc-side-logout" onClick={handleLogout}>
            <LogOut size={18} />
            <span>Logout</span>
          </button>
        </div>
      </aside>
    </>
  );
}
