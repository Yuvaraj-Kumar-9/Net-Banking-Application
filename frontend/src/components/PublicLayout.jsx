import { Outlet } from 'react-router-dom';
import Navbar from './Navbar';
import Footer from './Footer';

/** Shared chrome for the marketing and authentication pages. */
export default function PublicLayout() {
  return (
    <div className="hfdc-public-shell">
      <Navbar />
      <main className="hfdc-public-main">
        <Outlet />
      </main>
      <Footer />
    </div>
  );
}
