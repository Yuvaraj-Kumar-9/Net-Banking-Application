/**
 * Original HFDC Bank logo. The mark combines two pillars (H) with a bridge
 * arc, symbolising "Banking Beyond Boundaries". It is drawn as inline SVG so
 * it stays crisp at every size and needs no external asset.
 */
export default function Logo({ size = 40, light = false, showText = true }) {
  return (
    <span className="hfdc-logo">
      <svg
        width={size}
        height={size}
        viewBox="0 0 64 64"
        role="img"
        aria-label="HFDC Bank logo"
        className="hfdc-logo-mark"
      >
        <defs>
          <linearGradient id="hfdcLogoGradient" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stopColor="#123a6b" />
            <stop offset="100%" stopColor="#0b1f3a" />
          </linearGradient>
        </defs>
        <rect width="64" height="64" rx="16" fill="url(#hfdcLogoGradient)" />
        <path d="M19 19v26M45 19v26M19 32h26" stroke="#ffffff" strokeWidth="6" strokeLinecap="round" />
        <path d="M19 45c6-6 20-6 26 0" stroke="#63b3ed" strokeWidth="4" fill="none" strokeLinecap="round" />
      </svg>
      {showText && (
        <span className={`hfdc-logo-text ${light ? 'text-white' : ''}`}>
          <span className="hfdc-logo-name">HFDC</span>
          <span className="hfdc-logo-bank">BANK</span>
        </span>
      )}
    </span>
  );
}
