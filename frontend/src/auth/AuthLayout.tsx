import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { Activity, ShieldCheck, Sparkles, Timer } from 'lucide-react'
import { Logo } from '../components/ui'

export function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <div className="auth-page">
      <section className="auth-brand">
        <div className="auth-brand-top">
          <Logo />
          <span className="auth-pill">CS F213 · OOP project</span>
        </div>

        <div className="auth-brand-copy">
          <p className="eyebrow">Intelligent IT service management</p>
          <h1>
            Resolve incidents.
            <br />
            <em>Not spreadsheets.</em>
          </h1>
          <p>
            One workspace for reporting, triage, SLA guardrails, technician assignment and a complete
            audit trail — built around the way an IT support team actually works.
          </p>
        </div>

        <ul className="auth-benefits">
          <li>
            <span><Sparkles size={17} /></span>
            <div>
              <strong>Smart triage</strong>
              <p>Rule-based priority, category suggestions and duplicate detection.</p>
            </div>
          </li>
          <li>
            <span><Timer size={17} /></span>
            <div>
              <strong>SLA guardrails</strong>
              <p>Response and resolution targets with warnings, breaches and escalation.</p>
            </div>
          </li>
          <li>
            <span><ShieldCheck size={17} /></span>
            <div>
              <strong>Role-aware access</strong>
              <p>Employees, technicians and administrators each get the right workspace.</p>
            </div>
          </li>
        </ul>

        <div className="auth-pulse">
          <span className="pulse-dot" aria-hidden="true" />
          <div>
            <strong>Service desk online</strong>
            <span>13 demo incidents · 7 accounts · live PostgreSQL</span>
          </div>
          <Activity size={18} />
        </div>
      </section>

      <section className="auth-form-side">
        <div className="auth-mobile-brand">
          <Logo />
        </div>
        {children}
        <p className="auth-legal">
          I-ITSM demo · <Link to="/login">Sign in</Link> · <Link to="/register">Create employee account</Link>
        </p>
      </section>
    </div>
  )
}
