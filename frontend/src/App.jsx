import { useState } from 'react'
import { apiFetch } from './api.js'

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

  async function runTailor(downloadPdf) {
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
      const path = downloadPdf ? '/api/tailor/pdf' : '/api/tailor'
      const res = await apiFetch(path, { method: 'POST', body: form })
      if (!res.ok) {
        const body = await res.json().catch(() => ({}))
        throw new Error(body.error || `Request failed (${res.status})`)
      }
      if (downloadPdf) {
        const blob = await res.blob()
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = 'tailored-resume.pdf'
        a.click()
        URL.revokeObjectURL(url)
      } else {
        const data = await res.json()
        setResult(data)
      }
    } catch (e) {
      setError(e.message || 'Something went wrong')
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
      </header>

      <section className="card">
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
          <button type="button" disabled={loading} onClick={() => runTailor(false)}>
            {loading ? 'Generating resume & interview prep…' : 'Generate tailored resume'}
          </button>
          <button
            type="button"
            className="secondary"
            disabled={loading}
            onClick={() => runTailor(true)}
          >
            Download PDF
          </button>
        </div>
        {error && <p className="error">{error}</p>}
      </section>

      {result && (
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
