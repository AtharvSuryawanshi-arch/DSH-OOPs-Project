import { useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { ArrowRight, LoaderCircle, LockKeyhole, Mail, UserRound } from 'lucide-react'
import { errorMessage } from '../api/client'
import { useAuth } from './auth-context'
import { AuthLayout } from './AuthLayout'

export function RegisterPage() {
  const { user, register } = useAuth()
  const navigate = useNavigate()
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [department, setDepartment] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  if (user) {
    return <Navigate to="/" replace />
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (password !== confirmPassword) {
      setError('Passwords do not match.')
      return
    }
    if (password.length < 8) {
      setError('Password must be at least 8 characters.')
      return
    }

    setSubmitting(true)
    setError(null)
    try {
      await register({
        name: name.trim(),
        email: email.trim(),
        password,
        department: department.trim() || undefined,
      })
      navigate('/')
    } catch (cause) {
      setError(errorMessage(cause))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthLayout>
      <div className="auth-card">
        <div className="auth-heading">
          <p className="eyebrow">Employee self-service</p>
          <h2>Create your account</h2>
          <p>Register to report issues, follow their progress and verify resolutions.</p>
        </div>

        <form className="auth-form" onSubmit={handleSubmit}>
          <label className="field">
            <span className="field-label">Full name</span>
            <span className="input-with-icon">
              <UserRound size={17} />
              <input
                value={name}
                onChange={(event) => setName(event.target.value)}
                placeholder="Your name"
                autoComplete="name"
                required
                minLength={2}
                maxLength={120}
              />
            </span>
          </label>

          <label className="field">
            <span className="field-label">Work email</span>
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
            <span className="field-label">Department <span className="field-optional">optional</span></span>
            <span className="input-with-icon">
              <UserRound size={17} />
              <input
                value={department}
                onChange={(event) => setDepartment(event.target.value)}
                placeholder="Finance, Operations, Engineering…"
                maxLength={120}
              />
            </span>
          </label>

          <div className="form-grid-two">
            <label className="field">
              <span className="field-label">Password</span>
              <span className="input-with-icon">
                <LockKeyhole size={17} />
                <input
                  type="password"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  placeholder="At least 8 characters"
                  autoComplete="new-password"
                  minLength={8}
                  maxLength={72}
                  required
                />
              </span>
            </label>
            <label className="field">
              <span className="field-label">Confirm password</span>
              <span className="input-with-icon">
                <LockKeyhole size={17} />
                <input
                  type="password"
                  value={confirmPassword}
                  onChange={(event) => setConfirmPassword(event.target.value)}
                  placeholder="Repeat password"
                  autoComplete="new-password"
                  required
                />
              </span>
            </label>
          </div>

          {error && <div className="form-error">{error}</div>}

          <button type="submit" className="button button-primary button-block" disabled={submitting}>
            {submitting ? (
              <>
                <LoaderCircle className="spin" size={17} /> Creating account…
              </>
            ) : (
              <>
                Create account <ArrowRight size={17} />
              </>
            )}
          </button>
        </form>

        <p className="auth-switch">
          Already registered? <Link to="/login">Sign in</Link>
        </p>
      </div>
    </AuthLayout>
  )
}
