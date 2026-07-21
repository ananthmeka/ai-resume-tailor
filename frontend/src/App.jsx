import { useEffect, useState } from 'react'
import { apiFetch, getBetaToken, setBetaToken } from './api.js'

const LENGTH_OPTIONS = [
  { value: 'ONE_PAGE', label: 'One page' },
  { value: 'TWO_PAGES', label: 'Two pages' },
  { value: 'EXECUTIVE', label: 'Executive summary + detail' },
]

export default function App() {
  const [file, setFile] = useState(null)
  const [jobDescription, setJobDescription] = useState('')
  const [resumeLength, setResumeLength] = useState('TWO_PAGES')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [result, setResult] = useState(null)
  const [account, setAccount] = useState(null)
  const [accessCode, setAccessCode] = useState(getBetaToken())
  const [accessRequired, setAccessRequired] = useState(false)
  const [installPrompt, setInstallPrompt] = useState(null)

  async function loadAccount() {
    const res = await apiFetch('/api/account')
    if (res.status === 401) {
      setAccessRequired(true)
      setAccount(null)
      return false
    }
    if (!res.ok) {
      throw new Error(`Unable to contact the API (${res.status})`)
    }
    setAccount(await res.json())
    setAccessRequired(false)
    return true
  }

  useEffect(() => {
    loadAccount().catch((e) => setError(e.message))
    const captureInstall = (event) => {
      event.preventDefault()
      setInstallPrompt(event)
    }
    window.addEventListener('beforeinstallprompt', captureInstall)
    return () => window.removeEventListener('beforeinstallprompt', captureInstall)
  }, [])

  async function installApp() {
    if (!installPrompt) return
    await installPrompt.prompt()
    setInstallPrompt(null)
  }

  async function unlockBeta(event) {
    event.preventDefault()
    setError('')
    setBetaToken(accessCode)
    try {
      const valid = await loadAccount()
      if (!valid) {
        setBetaToken('')
        setError('That beta access code is not valid.')
      }
    } catch (e) {
      setError(e.message || 'Unable to validate the access code')
    }
  }

  function signOut() {
    setBetaToken('')
    setAccessCode('')
    setAccount(null)
    setAccessRequired(true)
    setResult(null)
  }

  async function runTailor() {
    setError('')
    if (!file) {
      setError('Please upload your base resume (PDF or DOCX).')
      return
    }
    if (!jobDescription.trim()) {
      setError('Please paste the job description.')
      return
    }
    setLoading(true)
    try {
      const form = new FormData()
      form.append('resume', file)
      form.append('jobDescription', jobDescription.trim())
      form.append('resumeLength', resumeLength)
      const res = await apiFetch('/api/tailor', { method: 'POST', body: form })
      if (!res.ok) {
        const body = await res.json().catch(() => ({}))
        throw new Error(body.error || `Request failed (${res.status})`)
      }
      const data = await res.json()
      setResult(data)
      loadAccount().catch(() => {})
    } catch (e) {
      setError(e.message || 'Something went wrong')
    } finally {
      setLoading(false)
    }
  }

  async function downloadPdf() {
    if (!result?.resultId) return
    setError('')
    setLoading(true)
    try {
      const res = await apiFetch(`/api/results/${result.resultId}/pdf`)
      if (!res.ok) {
        throw new Error(res.status === 404
          ? 'This download expired. Generate the resume again to create a fresh PDF.'
          : `PDF download failed (${res.status})`)
      }
      const blob = await res.blob()
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = 'tailored-resume.pdf'
      a.click()
      URL.revokeObjectURL(url)
    } catch (e) {
      setError(e.message || 'PDF download failed')
    } finally {
      setLoading(false)
    }
  }

  const scores = result?.analysis?.matchScores

  return (
    <div className="app">
      <header className="hero">
        <h1>AI Resume Tailor</h1>
        <p>
          One master resume, job-specific output. Your upload stays the source of truth — we reorder,
          highlight, and rewrite without inventing experience.
        </p>
        <p className="muted">
          <a href="/privacy.html">Privacy policy</a>
          {import.meta.env.VITE_API_BASE_URL ? (
            <> · API: {import.meta.env.VITE_API_BASE_URL}</>
          ) : (
            <> · Dev mode (local API)</>
          )}
        </p>
        {account && (
          <div className="account-bar">
            <span>
              Signed in as <strong>{account.user}</strong> · {account.monthlyRemaining} generations remaining this month
            </span>
            {account.authenticationRequired && <button className="link-button" onClick={signOut}>Sign out</button>}
          </div>
        )}
        {installPrompt && (
          <button className="install-button" type="button" onClick={installApp}>Install on this device</button>
        )}
      </header>

      {accessRequired && (
        <section className="card access-card">
          <h2>Small public beta</h2>
          <p>Enter the private access code issued to you. Your code is stored only in this browser.</p>
          <form onSubmit={unlockBeta}>
            <label htmlFor="accessCode">Beta access code</label>
            <div className="access-row">
              <input
                id="accessCode"
                type="password"
                value={accessCode}
                autoComplete="current-password"
                onChange={(e) => setAccessCode(e.target.value)}
              />
              <button type="submit" disabled={!accessCode.trim()}>Continue</button>
            </div>
          </form>
          {error && <p className="error">{error}</p>}
        </section>
      )}

      {!accessRequired && <section className="card">
        <label htmlFor="resume">Base resume (PDF, DOCX, TXT)</label>
        <input
          id="resume"
          type="file"
          accept=".pdf,.doc,.docx,.txt,application/pdf"
          onChange={(e) => setFile(e.target.files?.[0] ?? null)}
        />

        <label htmlFor="jd" style={{ marginTop: 16 }}>
          Job description
        </label>
        <textarea
          id="jd"
          placeholder="Paste the full job description here…"
          value={jobDescription}
          onChange={(e) => setJobDescription(e.target.value)}
        />

        <div className="length-row">
          <label>Target length</label>
          <div className="length-options">
            {LENGTH_OPTIONS.map((opt) => (
              <label key={opt.value}>
                <input
                  type="radio"
                  name="length"
                  value={opt.value}
                  checked={resumeLength === opt.value}
                  onChange={() => setResumeLength(opt.value)}
                />
                {opt.label}
              </label>
            ))}
          </div>
        </div>

        <div className="actions">
          <button type="button" disabled={loading} onClick={runTailor}>
            {loading ? 'Generating resume (may take up to ~90s on free Groq)…' : 'Generate tailored resume'}
          </button>
          <button
            type="button"
            className="secondary"
            disabled={loading || !result?.resultId}
            onClick={downloadPdf}
          >
            Download generated PDF (no extra AI usage)
          </button>
        </div>
        {error && <p className="error">{error}</p>}
      </section>}

      {!accessRequired && result && (
        <>
          <section className="card analysis">
            <h3>{result.analysis?.targetRoleSummary || 'Tailored for your target role'}</h3>
            {scores && (
              <div className="scores">
                <div className="score">
                  <strong>{scores.overall ?? '—'}%</strong>
                  Overall
                </div>
                <div className="score">
                  <strong>{scores.skills ?? '—'}%</strong>
                  Skills
                </div>
                <div className="score">
                  <strong>{scores.experience ?? '—'}%</strong>
                  Experience
                </div>
                <div className="score">
                  <strong>{scores.leadership ?? '—'}%</strong>
                  Leadership
                </div>
                <div className="score">
                  <strong>{scores.domain ?? '—'}%</strong>
                  Domain
                </div>
              </div>
            )}
            {result.analysis?.missingKeywords?.length > 0 && (
              <>
                <h4>Missing keywords (JD — not on resume)</h4>
                <p>{result.analysis.missingKeywords.join(', ')}</p>
              </>
            )}
            {result.analysis?.recommendations?.length > 0 && (
              <>
                <h4>Recommendations (truthful improvements)</h4>
                <ul>
                  {result.analysis.recommendations.map((r, i) => (
                    <li key={i}>{r}</li>
                  ))}
                </ul>
              </>
            )}
            {result.analysis?.changes?.length > 0 && (
              <>
                <h4>Change log</h4>
                <ul className="changelog">
                  {result.analysis.changes.map((c, i) => (
                    <li key={i}>
                      <strong>{c.area}:</strong> {c.change}
                      <br />
                      <span className="muted">Why: {c.reason}</span>
                    </li>
                  ))}
                </ul>
              </>
            )}
          </section>

          {result.interviewPrep?.questions?.length > 0 && (
            <section className="card interview-prep">
              <h3>Sample interview questions (JD + your background)</h3>
              <p className="muted">
                Grounded in this job description and your resume — use STAR stories from real roles and projects.
              </p>
              <ul className="interview-list">
                {result.interviewPrep.questions.map((q, i) => (
                  <li key={i}>
                    <span className="interview-category">{q.category}</span>
                    <p className="interview-q">{q.question}</p>
                    {q.groundedIn && <p className="muted">Based on: {q.groundedIn}</p>}
                    {q.rationale && <p className="interview-why">{q.rationale}</p>}
                  </li>
                ))}
              </ul>
            </section>
          )}

          <div className="grid">
            <section className="card">
              <h3>Original text (extract snippet)</h3>
              <p className="muted">First ~8,000 characters of parsed file — full text is used for tailoring.</p>
              <div className="original-preview">{result.originalTextPreview}</div>
            </section>
            <section className="card">
              <h3>Tailored resume preview</h3>
              <iframe
                className="preview-frame"
                title="Resume preview"
                srcDoc={result.htmlResume}
                sandbox="allow-same-origin"
              />
            </section>
          </div>
        </>
      )}
    </div>
  )
}
