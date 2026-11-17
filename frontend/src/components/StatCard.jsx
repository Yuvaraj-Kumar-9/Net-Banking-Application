/** Dashboard statistic card with an icon and optional trend note. */
export default function StatCard({ icon: Icon, label, value, hint, tone = 'navy' }) {
  return (
    <div className={`hfdc-stat-card hfdc-stat-${tone}`}>
      <div className="hfdc-stat-icon">{Icon && <Icon size={22} />}</div>
      <div className="min-w-0">
        <div className="hfdc-stat-label">{label}</div>
        <div className="hfdc-stat-value">{value}</div>
        {hint && <div className="hfdc-stat-hint">{hint}</div>}
      </div>
    </div>
  );
}
