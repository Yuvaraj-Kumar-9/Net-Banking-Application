import { Link } from 'react-router-dom';
import { Home, ArrowLeft } from 'lucide-react';
import Logo from '../components/Logo';
import Disclaimer from '../components/Disclaimer';

/** 404 page for unknown routes. */
export default function NotFound() {
  return (
    <div className="hfdc-notfound">
      <div className="hfdc-notfound-card">
        <Logo size={52} />
        <p className="hfdc-notfound-code">404</p>
        <h1>This page has left the branch</h1>
        <p>
          The address you followed does not exist in HFDC Bank. It may have been moved, or the
          link may be mistyped.
        </p>
        <div className="d-flex flex-column flex-sm-row gap-3 justify-content-center">
          <Link to="/" className="btn btn-primary hfdc-btn">
            <Home size={16} /> Back to home
          </Link>
          <Link to="/dashboard" className="btn btn-outline-primary hfdc-btn">
            <ArrowLeft size={16} /> Open dashboard
          </Link>
        </div>
        <div className="mt-4">
          <Disclaimer compact />
        </div>
      </div>
    </div>
  );
}
