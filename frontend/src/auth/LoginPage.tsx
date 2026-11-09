import { useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { ArrowRight, Eye, EyeOff, LoaderCircle, LockKeyhole, Mail } from 'lucide-react'
import { errorMessage } from '../api/client'
import { useAuth } from './auth-context'
import { AuthLayout } from './AuthLayout'

const demoAccounts = [
  { label: 'Admin', email: 'admin@itsm.local', password: 'Admin@123', tone: 'violet' },
  { label: 'Technician', email: 'priya@itsm.local', password: 'Tech@123', tone: 'blue' },
  { label: 'Employee', email: 'employee@itsm.local', password: 'Employee@123', tone: 'green' },
]

export function LoginPage() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  if (user) {
    return <Navigate to="/" replace />
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!email.trim() || !password) {
      return
    }
    setSubmitting(true)
    setError(null)
    try {
      await login(email.trim(), password)
      navigate('/')
    } catch (cause) {
      setError(errorMessage(cause))
    } finally {
      setSubmitting(false)
    }
  }

  function fillDemo(account: (typeof demoAccounts)[number]) {
    setEmail(account.email)
    setPassword(account.password)
    setError(null)
  }

  return (
    <AuthLayout>
      <div className="auth-card">
        <div className="auth-heading">
          <p className="eyebrow">Welcome back</p>
          <h2>Sign in to I-ITSM</h2>
          <p>Use your service desk account. Demo credentials are available below.</p>
        </div>

        <form className="auth-form" onSubmit={handleSubmit}>
          <label className="field">
            <span className="field-label">Email address</span>
            <span className="input-with-icon">
              <Mail size={17} />
              <input
                type="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                placeholder="you@organisation.com"
                autoComplete="email"
                required
              />
            </span>
          </label>

          <label className="field">
            <span className="field-label">Password</span>
            <span className="input-with-icon">
              <LockKeyhole size={17} />
              <input
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                placeholder="Enter your password"
                autoComplete="current-password"
                required
              />
              <button
                type="button"
                className="input-trailing-button"
                onClick={() => setShowPassword((current) => !current)}
                aria-label={showPassword ? 'Hide password' : 'Show password'}
              >
                {showPassword ? <EyeOff size={17} /> : <Eye size={17} />}
              </button>
            </span>
          </label>

          {error && <div className="form-error">{error}</div>}

          <button type="submit" className="button button-primary button-block" disabled={submitting}>
            {submitting ? (
              <>
                <LoaderCircle className="spin" size={17} /> Signing in…
              </>
            ) : (
              <>
                Sign in <ArrowRight size={17} />
              </>
            )}
          </button>
        </form>

        <div className="demo-accounts">
          <div className="demo-accounts-heading">
            <span>Explore the demo</span>
            <span>one-click fill</span>
          </div>
          <div className="demo-account-grid">
            {demoAccounts.map((account) => (
              <button
                key={account.label}
                type="button"
                className={`demo-account demo-${account.tone}`}
                onClick={() => fillDemo(account)}
              >
                <strong>{account.label}</strong>
                <span>{account.email}</span>
              </button>
            ))}
          </div>
        </div>

        <p className="auth-switch">
          New employee? <Link to="/register">Create an account</Link>
        </p>
      </div>
    </AuthLayout>
  )
}
