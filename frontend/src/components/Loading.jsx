/** Accessible loading indicator used while API data is in flight. */
export default function Loading({ label = 'Loading secure data…', fullScreen = false }) {
  const content = (
    <div className="d-flex flex-column align-items-center justify-content-center gap-3 py-5">
      <div className="hfdc-spinner" role="status" aria-label={label}>
        <span className="visually-hidden">{label}</span>
      </div>
      <p className="text-muted mb-0 small">{label}</p>
    </div>
  );
  if (fullScreen) {
    return <div className="hfdc-loading-screen">{content}</div>;
  }
  return content;
}
