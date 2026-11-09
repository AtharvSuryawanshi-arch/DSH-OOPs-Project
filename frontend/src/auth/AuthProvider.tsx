import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { authApi, getStoredToken, storeToken } from '../api/client'
import type { User } from '../api/types'
import { AuthContext, type AuthContextValue } from './auth-context'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(() => Boolean(getStoredToken()))

  useEffect(() => {
    if (!getStoredToken()) {
      return
    }
    let cancelled = false

    async function restoreSession() {
      try {
        const currentUser = await authApi.me()
        if (!cancelled) {
          setUser(currentUser)
        }
      } catch {
        if (!cancelled) {
          storeToken(null)
          setUser(null)
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    void restoreSession()

    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    function handleUnauthorized() {
      storeToken(null)
      setUser(null)
      setLoading(false)
    }

    window.addEventListener('itsm:unauthorized', handleUnauthorized)
    return () => window.removeEventListener('itsm:unauthorized', handleUnauthorized)
  }, [])

  const login = useCallback(async (email: string, password: string) => {
    const response = await authApi.login(email, password)
    storeToken(response.token)
    setUser(response.user)
    setLoading(false)
    return response.user
  }, [])

  const register = useCallback(
    async (payload: { name: string; email: string; password: string; department?: string }) => {
      const response = await authApi.register(payload)
      storeToken(response.token)
      setUser(response.user)
      setLoading(false)
      return response.user
    },
    [],
  )

  const logout = useCallback(() => {
    storeToken(null)
    setUser(null)
  }, [])

  const refreshUser = useCallback(async () => {
    const currentUser = await authApi.me()
    setUser(currentUser)
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({ user, loading, login, register, logout, refreshUser }),
    [user, loading, login, register, logout, refreshUser],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
