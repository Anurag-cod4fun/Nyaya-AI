import { useState } from 'react'
import './App.css'

const API_URL = 'http://localhost:8080'
const examples = [
  'What is reasonable compensation for breach of contract?',
  'When can a penalty clause be enforced under Section 74?',
  'How is loss assessed under Section 73 of the Contract Act?',
]

function App() {
  const [question, setQuestion] = useState(examples[0])
  const [limit, setLimit] = useState(3)
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  async function askQuestion(event) {
    event?.preventDefault()
    if (!question.trim() || loading) return
    setLoading(true); setError(''); setResult(null)
    try {
      const response = await fetch(`${API_URL}/api/ask`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ question: question.trim(), limit }),
      })
      if (!response.ok) {
        throw new Error(response.status === 429 ? 'Vertex AI is temporarily rate-limited. Please try again shortly.' : `The research service returned ${response.status}.`)
      }
      setResult(await response.json())
    } catch (requestError) { setError(requestError.message || 'Could not reach the research service.') }
    finally { setLoading(false) }
  }

  return <main className="app-shell">
    <header className="topbar"><a className="brand" href="/" aria-label="Nyaya AI home"><span className="brand-mark">न्या</span><span><strong>NYAYA</strong><small>LEGAL INTELLIGENCE</small></span></a><div className="status"><span className="status-dot" /> Research workspace <span className="status-divider" /> India</div></header>
    <section className="intro"><div className="kicker">CASE LAW / STATUTES / PRECEDENT</div><h1>Ask the law.<br /><em>See the authority.</em></h1><p>Grounded research across Indian statutes and High Court &amp; Supreme Court judgments.</p></section>
    <section className="research-grid">
      <aside className="question-rail"><div className="rail-label"><span>01</span> RESEARCH QUERY</div><form onSubmit={askQuestion}><label htmlFor="question">Your question</label><textarea id="question" value={question} onChange={(event) => setQuestion(event.target.value)} placeholder="Ask a legal question..." rows="7" /><div className="controls"><label htmlFor="sources">Sources</label><select id="sources" value={limit} onChange={(event) => setLimit(Number(event.target.value))}><option value="3">3 passages</option><option value="5">5 passages</option><option value="8">8 passages</option></select></div><button className="ask-button" type="submit" disabled={loading || !question.trim()}><span>{loading ? 'Researching...' : 'Research question'}</span><span className="button-arrow">↗</span></button></form><div className="examples"><div className="rail-label"><span>02</span> TRY A QUERY</div>{examples.map((example) => <button key={example} className="example" onClick={() => setQuestion(example)} type="button"><span>↳</span>{example}</button>)}</div></aside>
      <section className="answer-area" aria-live="polite">
        {!result && !loading && !error && <div className="empty-state"><div className="seal">न्या</div><div><span className="empty-kicker">READY TO RESEARCH</span><h2>Your answer will appear here.</h2><p>Ask a question to retrieve relevant authority and generate a cited synthesis.</p></div></div>}
        {loading && <div className="loading-state"><div className="loader" /><span>Searching the corpus and consulting Gemini</span><small>This can take a moment</small></div>}
        {error && <div className="error-state"><span className="error-icon">!</span><div><strong>Research unavailable</strong><p>{error}</p><button type="button" onClick={askQuestion}>Try again</button></div></div>}
        {result && !loading && <><div className="answer-heading"><div><span className="empty-kicker">SYNTHESIS</span><h2>Research answer</h2></div><span className="answer-count">{result.sources?.length || 0} sources</span></div><article className="answer-card"><div className="answer-rule" /><div className="answer-copy">{result.answer.split('\n').map((paragraph, index) => paragraph.trim() && <p key={`${paragraph}-${index}`}>{paragraph}</p>)}</div><div className="answer-note">AI-assisted research · Verify against the cited primary text before relying on this analysis.</div></article><div className="sources-heading"><div><span className="empty-kicker">AUTHORITY</span><h2>Retrieved sources</h2></div><span className="sort-note">Ranked by semantic relevance</span></div><div className="source-list">{result.sources?.map((source, index) => <SourceCard key={source.chunkId} source={source} index={index} />)}</div></>}
      </section>
    </section>
    <footer><span>NYAYA AI</span><span>Retrieval-augmented legal research</span><span>Local corpus · Gemini synthesis</span></footer>
  </main>
}

function SourceCard({ source, index }) { return <article className="source-card"><div className="source-index">S{index + 1}</div><div className="source-body"><div className="source-meta"><span>{source.court?.replaceAll('_', ' ')}</span><span>Page {source.pageNumber || '—'}</span><span>Distance {source.distance.toFixed(3)}</span></div><h3>{source.caseName}</h3><p className="citation">{source.citation || 'Statutory source'} · {source.documentId}</p><p className="source-content">“{source.content}”</p></div></article> }
export default App
