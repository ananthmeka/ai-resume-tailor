/**
 * API base for production (Vercel → Railway / tunnel).
 * Dev: leave unset; Vite proxies /api to localhost:8080.
 */
export function getApiBase() {
  const base = import.meta.env.VITE_API_BASE_URL?.trim()
  if (!base) {
    return ''
  }
  return base.replace(/\/$/, '')
}

export function apiUrl(path) {
  const p = path.startsWith('/') ? path : `/${path}`
  return `${getApiBase()}${p}`
}

/** Public beta key (visible in static JS — use only with rate limits; rotate often). */
export function apiHeaders(extra = {}) {
  const headers = { ...extra }
  const key = import.meta.env.VITE_API_KEY?.trim()
  if (key) {
    headers['X-API-Key'] = key
  }
  return headers
}

export function apiFetch(path, options = {}) {
  const headers = apiHeaders(options.headers || {})
  return fetch(apiUrl(path), { ...options, headers })
}
