/** Consistent page heading with optional action area. */
export default function PageHeader({ title, subtitle, actions }) {
  return (
    <div className="hfdc-page-header">
      <div>
        <h2>{title}</h2>
        {subtitle && <p>{subtitle}</p>}
      </div>
      {actions && <div className="hfdc-page-actions">{actions}</div>}
    </div>
  );
}
