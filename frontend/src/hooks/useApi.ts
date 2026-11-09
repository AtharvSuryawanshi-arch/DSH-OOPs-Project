import { useCallback, useEffect, useState } from 'react'
import { apiGet, errorMessage } from '../api/client'

interface QueryState<T> {
  path: string
  data: T | null
  loading: boolean
  error: string | null
}

export function useApi<T>(path: string) {
  const [state, setState] = useState<QueryState<T>>({
    path,
    data: null,
    loading: true,
    error: null,
  })
  const [version, setVersion] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    let cancelled = false

    async function load() {
      try {
        const data = await apiGet<T>(path, controller.signal)
        if (!cancelled) {
          setState({ path, data, loading: false, error: null })
        }
      } catch (cause) {
        if (cancelled || (cause instanceof DOMException && cause.name === 'AbortError')) {
          return
        }
        setState((previous) => ({
          path,
          data: previous.path === path ? previous.data : null,
          loading: false,
          error: errorMessage(cause),
        }))
      }
    }

    void load()

    return () => {
      cancelled = true
      controller.abort()
    }
  }, [path, version])

  const loading = state.path !== path || state.loading
  const data = state.path === path ? state.data : null
  const error = state.path === path ? state.error : null

  const refetch = useCallback(() => {
    setState((previous) => ({ ...previous, loading: true }))
    setVersion((current) => current + 1)
  }, [])

  return { data, loading, error, refetch }
}
