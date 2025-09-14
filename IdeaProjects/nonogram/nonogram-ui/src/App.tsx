import { useEffect, useState } from 'react'

type SolutionDto = { height: number; width: number; filled: boolean[][] }

export default function App() {
  const [puzzles, setPuzzles] = useState<string[]>([])
  const [solution, setSolution] = useState<SolutionDto | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    // 1) Получаем список встроенных пазлов
    fetch('/api/puzzles')
      .then(r => {
        if (!r.ok) throw new Error(`/api/puzzles ${r.status}`)
        return r.json()
      })
      .then(setPuzzles)
      .catch(e => setError(e.message))
  }, [])

  const solveFirst = async () => {
    setError(null)
    setSolution(null)
    try {
      if (!puzzles.length) throw new Error('Нет встроенных пазлов')
      const name = puzzles[0]
      const res = await fetch(`/api/solve/builtin/${encodeURIComponent(name)}`)
      if (!res.ok) throw new Error(`solve/builtin ${res.status}`)
      const json = await res.json()
      setSolution(json)
    } catch (e: any) {
      setError(e.message)
    }
  }

  return (
    <div style={{ fontFamily: 'system-ui', padding: 16 }}>
      <h1>Nonogram UI</h1>

      <section>
        <h2>Встроенные пазлы</h2>
        {error && <p style={{ color: 'crimson' }}>Ошибка: {error}</p>}
        {!puzzles.length ? <p>Загрузка...</p> : (
          <>
            <ul>
              {puzzles.map(p => <li key={p}>{p}</li>)}
            </ul>
            <button onClick={solveFirst}>Решить первый</button>
          </>
        )}
      </section>

      {solution && (
        <section style={{ marginTop: 16 }}>
          <h2>Решение</h2>
          <p>{solution.width}×{solution.height}</p>
          <div style={{ display: 'grid', gridTemplateColumns: `repeat(${solution.width}, 18px)`, gap: 2 }}>
            {solution.filled.flat().map((cell, i) => (
              <div key={i} style={{
                width: 18, height: 18,
                background: cell ? '#222' : '#eee',
                border: '1px solid #ccc'
              }} />
            ))}
          </div>
        </section>
      )}
    </div>
  )
}
