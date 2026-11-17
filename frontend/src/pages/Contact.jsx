import { useState } from 'react';
import { Mail, MapPin, Phone, Send, ShieldAlert, CheckCircle2 } from 'lucide-react';
import { useNotify } from '../context/NotificationContext';
import Disclaimer from '../components/Disclaimer';

const INITIAL = { name: '', email: '', subject: '', message: '' };

/**
 * Contact form. It validates locally and shows a confirmation, but deliberately
 * does not pretend to email a real bank - this is a simulation.
 */
export default function Contact() {
  const [form, setForm] = useState(INITIAL);
  const [errors, setErrors] = useState({});
  const [sent, setSent] = useState(false);
  const { notify } = useNotify();

  const update = (field) => (event) => {
    setForm((current) => ({ ...current, [field]: event.target.value }));
    setErrors((current) => ({ ...current, [field]: undefined }));
  };

  const validate = () => {
    const next = {};
    if (!form.name.trim()) next.name = 'Please enter your name';
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
      next.email = 'Please enter a valid email address';
    }
    if (!form.subject.trim()) next.subject = 'Please choose a subject';
    if (form.message.trim().length < 10) next.message = 'Please write at least 10 characters';
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const submit = (event) => {
    event.preventDefault();
    if (!validate()) return;
    // No backend endpoint exists: this is a demonstration form only.
    setSent(true);
    notify('Message captured locally. This demo does not send email.', 'info');
  };

  return (
    <>
      <section className="hfdc-page-hero">
        <div className="container">
          <span className="hfdc-eyebrow">Contact</span>
          <h1>Talk to the project team</h1>
          <p>
            Questions about the architecture, the security model or the demo data? Send a note
            below - or read the FAQ on the home page first.
          </p>
          <Disclaimer />
        </div>
      </section>

      <section className="hfdc-section">
        <div className="container">
          <div className="row g-5">
            <div className="col-lg-5">
              <h2 className="hfdc-section-title">Demo support channels</h2>
              <p className="text-muted">
                These contact details are placeholders for the educational project. No real bank
                is reachable through them.
              </p>

              <ul className="hfdc-contact-list">
                <li>
                  <span className="hfdc-contact-icon"><Mail size={18} /></span>
                  <div>
                    <strong>Email</strong>
                    <span>support@hfdcbank.test</span>
                  </div>
                </li>
                <li>
                  <span className="hfdc-contact-icon"><Phone size={18} /></span>
                  <div>
                    <strong>Phone</strong>
                    <span>+91 00000 00000 (placeholder)</span>
                  </div>
                </li>
                <li>
                  <span className="hfdc-contact-icon"><MapPin size={18} /></span>
                  <div>
                    <strong>Address</strong>
                    <span>1 Demo Avenue, Mumbai, India</span>
                  </div>
                </li>
              </ul>

              <div className="hfdc-notice">
                <ShieldAlert size={18} />
                <p>
                  Never send real passwords, PINs, card numbers or identity documents through this
                  form. It is a static demonstration and stores nothing on a server.
                </p>
              </div>
            </div>

            <div className="col-lg-7">
              <div className="hfdc-form-card">
                {sent ? (
                  <div className="hfdc-success">
                    <div className="hfdc-success-icon">
                      <CheckCircle2 size={30} />
                    </div>
                    <h3>Thanks, {form.name.split(' ')[0]}!</h3>
                    <p>
                      Your message was validated and captured in this browser only. Because HFDC
                      Bank is a simulation, no email was sent and no support ticket was created.
                    </p>
                    <button
                      type="button"
                      className="btn btn-primary hfdc-btn"
                      onClick={() => {
                        setForm(INITIAL);
                        setSent(false);
                      }}
                    >
                      Write another message
                    </button>
                  </div>
                ) : (
                  <form onSubmit={submit} noValidate>
                    <h2 className="hfdc-form-title">Send a message</h2>
                    <p className="hfdc-form-sub">All fields are required.</p>

                    <div className="row g-3">
                      <div className="col-md-6">
                        <label className="form-label" htmlFor="contact-name">Full name</label>
                        <input
                          id="contact-name"
                          type="text"
                          className={`form-control ${errors.name ? 'is-invalid' : ''}`}
                          value={form.name}
                          onChange={update('name')}
                          autoComplete="name"
                        />
                        {errors.name && <div className="invalid-feedback">{errors.name}</div>}
                      </div>
                      <div className="col-md-6">
                        <label className="form-label" htmlFor="contact-email">Email address</label>
                        <input
                          id="contact-email"
                          type="email"
                          className={`form-control ${errors.email ? 'is-invalid' : ''}`}
                          value={form.email}
                          onChange={update('email')}
                          autoComplete="email"
                        />
                        {errors.email && <div className="invalid-feedback">{errors.email}</div>}
                      </div>
                      <div className="col-12">
                        <label className="form-label" htmlFor="contact-subject">Subject</label>
                        <select
                          id="contact-subject"
                          className={`form-select ${errors.subject ? 'is-invalid' : ''}`}
                          value={form.subject}
                          onChange={update('subject')}
                        >
                          <option value="">Choose a subject…</option>
                          <option>General question</option>
                          <option>Feedback on the UI</option>
                          <option>Question about the code</option>
                          <option>Report a bug</option>
                        </select>
                        {errors.subject && <div className="invalid-feedback">{errors.subject}</div>}
                      </div>
                      <div className="col-12">
                        <label className="form-label" htmlFor="contact-message">Message</label>
                        <textarea
                          id="contact-message"
                          rows={5}
                          className={`form-control ${errors.message ? 'is-invalid' : ''}`}
                          value={form.message}
                          onChange={update('message')}
                          placeholder="Tell us what you are thinking…"
                        />
                        {errors.message && <div className="invalid-feedback">{errors.message}</div>}
                      </div>
                    </div>

                    <button type="submit" className="btn btn-primary hfdc-btn mt-4">
                      <Send size={16} /> Send message
                    </button>
                    <p className="hfdc-form-note">
                      Demo form: nothing is transmitted to a server.
                    </p>
                  </form>
                )}
              </div>
            </div>
          </div>
        </div>
      </section>
    </>
  );
}
