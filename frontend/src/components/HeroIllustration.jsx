/**
 * Original vector illustration for the landing hero: a banking card, a phone
 * with a mini chart and floating security/rupee badges. Pure SVG, no assets.
 */
export default function HeroIllustration() {
  return (
    <svg
      viewBox="0 0 560 460"
      className="hfdc-hero-illustration"
      role="img"
      aria-label="Illustration of the HFDC Bank mobile banking dashboard"
    >
      <defs>
        <linearGradient id="heroBlob" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor="#dbeafe" />
          <stop offset="100%" stopColor="#eff6ff" />
        </linearGradient>
        <linearGradient id="cardGradient" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor="#123a6b" />
          <stop offset="100%" stopColor="#0b1f3a" />
        </linearGradient>
        <linearGradient id="phoneGradient" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#ffffff" />
          <stop offset="100%" stopColor="#f8fbff" />
        </linearGradient>
        <filter id="softShadow" x="-20%" y="-20%" width="140%" height="140%">
          <feDropShadow dx="0" dy="14" stdDeviation="16" floodColor="#0b1f3a" floodOpacity="0.18" />
        </filter>
      </defs>

      <path
        d="M84 238c-26-80 8-166 82-206 74-40 173-30 241 22 68 52 94 145 56 218-38 73-124 112-214 104C159 368 110 318 84 238Z"
        fill="url(#heroBlob)"
      />

      {/* Floating bank card */}
      <g transform="translate(38 96) rotate(-8)" filter="url(#softShadow)">
        <rect width="300" height="180" rx="20" fill="url(#cardGradient)" />
        <text x="26" y="42" fill="#ffffff" fontSize="20" fontWeight="800" letterSpacing="1">
          HFDC BANK
        </text>
        <text x="26" y="64" fill="#93c5fd" fontSize="10" letterSpacing="2">
          BANKING BEYOND BOUNDARIES
        </text>
        <rect x="26" y="92" width="42" height="30" rx="6" fill="#f4c95d" />
        <path d="M26 107h42M47 92v30" stroke="#c9a227" strokeWidth="1.5" />
        <text x="26" y="150" fill="#e2e8f0" fontSize="15" letterSpacing="2">
          •••• •••• •••• 4821
        </text>
        <text x="220" y="150" fill="#e2e8f0" fontSize="11">VALID 12/29</text>
        <circle cx="252" cy="36" r="14" fill="#63b3ed" opacity="0.85" />
        <circle cx="270" cy="36" r="14" fill="#93c5fd" opacity="0.6" />
      </g>

      {/* Phone mock-up */}
      <g transform="translate(280 52)" filter="url(#softShadow)">
        <rect width="196" height="352" rx="30" fill="#0b1f3a" />
        <rect x="8" y="8" width="180" height="336" rx="24" fill="url(#phoneGradient)" />
        <rect x="72" y="16" width="52" height="8" rx="4" fill="#cbd5e1" />

        <text x="24" y="58" fill="#64748b" fontSize="10">AVAILABLE BALANCE</text>
        <text x="24" y="86" fill="#0b1f3a" fontSize="24" fontWeight="800">₹1,15,000</text>
        <rect x="24" y="98" width="60" height="20" rx="10" fill="#dcfce7" />
        <text x="34" y="112" fill="#15803d" fontSize="9" fontWeight="700">+12.4%</text>

        {/* Mini bar chart */}
        <g transform="translate(24 136)">
          <line x1="0" y1="92" x2="148" y2="92" stroke="#e2e8f0" strokeWidth="1" />
          <rect x="4" y="52" width="16" height="40" rx="4" fill="#bfdbfe" />
          <rect x="30" y="34" width="16" height="58" rx="4" fill="#60a5fa" />
          <rect x="56" y="60" width="16" height="32" rx="4" fill="#bfdbfe" />
          <rect x="82" y="22" width="16" height="70" rx="4" fill="#123a6b" />
          <rect x="108" y="42" width="16" height="50" rx="4" fill="#60a5fa" />
          <rect x="134" y="14" width="16" height="78" rx="4" fill="#0b1f3a" />
        </g>

        {/* Recent rows */}
        <g transform="translate(24 252)">
          <rect width="148" height="34" rx="10" fill="#eff6ff" />
          <circle cx="18" cy="17" r="9" fill="#dbeafe" />
          <path d="M14 17h8M18 13v8" stroke="#1d4ed8" strokeWidth="1.5" strokeLinecap="round" />
          <text x="34" y="14" fill="#0b1f3a" fontSize="9" fontWeight="700">Deposit</text>
          <text x="34" y="26" fill="#64748b" fontSize="8">Today</text>
          <text x="146" y="21" fill="#15803d" fontSize="10" fontWeight="700" textAnchor="end">+₹5,000</text>
        </g>
        <g transform="translate(24 294)">
          <rect width="148" height="34" rx="10" fill="#fff7ed" />
          <circle cx="18" cy="17" r="9" fill="#ffedd5" />
          <path d="M14 17h8" stroke="#c2410c" strokeWidth="1.5" strokeLinecap="round" />
          <text x="34" y="14" fill="#0b1f3a" fontSize="9" fontWeight="700">Transfer</text>
          <text x="34" y="26" fill="#64748b" fontSize="8">Yesterday</text>
          <text x="146" y="21" fill="#c2410c" fontSize="10" fontWeight="700" textAnchor="end">-₹2,500</text>
        </g>
      </g>

      {/* Security badge */}
      <g transform="translate(78 330)" filter="url(#softShadow)">
        <rect width="150" height="56" rx="16" fill="#ffffff" />
        <circle cx="28" cy="28" r="16" fill="#dcfce7" />
        <path d="M28 19l9 4v6c0 6-4 9-9 11-5-2-9-5-9-11v-6l9-4Z" fill="#16a34a" opacity="0.2" />
        <path d="M24 28l3 3 5-6" stroke="#16a34a" strokeWidth="2" fill="none" strokeLinecap="round" />
        <text x="52" y="24" fill="#0b1f3a" fontSize="11" fontWeight="800">Secure banking</text>
        <text x="52" y="39" fill="#64748b" fontSize="9">JWT · BCrypt · CSRF</text>
      </g>

      {/* Rupee badge */}
      <g transform="translate(468 296)" filter="url(#softShadow)">
        <circle cx="34" cy="34" r="34" fill="#ffffff" />
        <circle cx="34" cy="34" r="26" fill="#dbeafe" />
        <text x="34" y="44" textAnchor="middle" fontSize="28" fontWeight="800" fill="#123a6b">₹</text>
      </g>
    </svg>
  );
}
