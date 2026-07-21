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

const BETA_TOKEN_KEY = 'resume-tailor-beta-token'

export function getBetaToken() {
  return window.localStorage.getItem(BETA_TOKEN_KEY) || ''
}

export function setBetaToken(token) {
  if (token?.trim()) {
    window.localStorage.setItem(BETA_TOKEN_KEY, token.trim())
  } else {
    window.localStorage.removeItem(BETA_TOKEN_KEY)
  }
}

export function apiHeaders(extra = {}) {
  const headers = { ...extra }
  const token = getBetaToken()
  if (token) {
    headers['X-Beta-Token'] = token
  }
  return headers
}

export function apiFetch(path, options = {}) {
  const headers = apiHeaders(options.headers || {})
  return fetch(apiUrl(path), { ...options, headers })
}
