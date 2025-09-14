import React, { useEffect, useMemo, useRef, useState } from "react";

// --- Types ---
type SolveResponse = {
  filled: boolean[][];
};

// --- Config ---
const API_BASE = "http://localhost:8080/api"; // adjust if needed

// --- Small API helpers (inline for v1; you can later move to src/api.ts) ---
async function getBuiltinList(): Promise<string[]> {
  const res = await fetch(`${API_BASE}/builtin/list`);
  if (!res.ok) throw new Error(`Failed to load list: ${res.status}`);
  return res.json();
}

async function solveBuiltin(name: string): Promise<SolveResponse> {
  const res = await fetch(`${API_BASE}/solve/builtin/${encodeURIComponent(name)}`);
  if (!res.ok) throw new Error(`Solve failed: ${res.status}`);
  return res.json();
}

// --- Grid component ---
const Grid: React.FC<{
  data: boolean[][];
  cellSize?: number; // in px
}> = ({ data, cellSize = 22 }) => {
  const rows = data.length;
  const cols = rows ? data[0].length : 0;

  // Create a flat array for rendering via CSS grid
  const flat = useMemo(() => data.flat(), [data]);

  return (
    <div className="w-full overflow-auto">
      <div
        className="inline-grid rounded-2xl shadow-sm border border-gray-200"
        style={{
          gridTemplateColumns: `repeat(${cols}, ${cellSize}px)`,
          gridAutoRows: `${cellSize}px`,
        }}
      >
        {flat.map((filled, i) => (
          <div
            key={i}
            className={`border border-gray-200/60 ${filled ? "bg-gray-900" : "bg-white"}`}
            aria-label={filled ? "filled" : "empty"}
          />
        ))}
      </div>
    </div>
  );
};

// --- App ---
const App: React.FC = () => {
  const [list, setList] = useState<string[]>([]);
  const [loadingList, setLoadingList] = useState(false);
  const [errorList, setErrorList] = useState<string | null>(null);

  const [selected, setSelected] = useState<string | null>(null);
  const [solving, setSolving] = useState(false);
  const [errorSolve, setErrorSolve] = useState<string | null>(null);
  const [solution, setSolution] = useState<boolean[][] | null>(null);

  const [cellSize, setCellSize] = useState(22);

  useEffect(() => {
    let aborted = false;
    setLoadingList(true);
    setErrorList(null);
    getBuiltinList()
      .then((names) => {
        if (aborted) return;
        setList(names);
      })
      .catch((e) => !aborted && setErrorList(String(e)))
      .finally(() => !aborted && setLoadingList(false));
    return () => {
      aborted = true;
    };
  }, []);

  const handleSolve = async (name: string) => {
    setSelected(name);
    setSolving(true);
    setErrorSolve(null);
    setSolution(null);
    try {
      const res = await solveBuiltin(name);
      setSolution(res.filled);
    } catch (e: any) {
      setErrorSolve(String(e));
    } finally {
      setSolving(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 text-gray-900">
      <header className="sticky top-0 z-10 bg-white/80 backdrop-blur border-b border-gray-200">
        <div className="max-w-6xl mx-auto px-4 py-3 flex items-center justify-between">
          <h1 className="text-xl font-semibold">Nonogram UI</h1>
          <div className="flex items-center gap-4">
            <label className="text-sm flex items-center gap-2">
              Cell size
              <input
                type="range"
                min={12}
                max={40}
                value={cellSize}
                onChange={(e) => setCellSize(parseInt(e.target.value))}
              />
              <span className="tabular-nums text-xs text-gray-600">{cellSize}px</span>
            </label>
            <a
              className="text-sm text-gray-600 hover:text-gray-900 underline"
              href="http://localhost:8080/swagger-ui/index.html"
              target="_blank"
              rel="noreferrer"
            >
              API docs
            </a>
          </div>
        </div>
      </header>

      <main className="max-w-6xl mx-auto px-4 py-6 grid grid-cols-1 md:grid-cols-12 gap-6">
        {/* Sidebar list */}
        <aside className="md:col-span-4 lg:col-span-3">
          <div className="bg-white rounded-2xl border border-gray-200 shadow-sm p-3">
            <h2 className="text-sm font-semibold mb-2">Built‑in puzzles</h2>
            {loadingList && <p className="text-sm text-gray-500">Loading…</p>}
            {errorList && (
              <p className="text-sm text-red-600">{errorList}</p>
            )}
            <ul className="space-y-1 max-h-[60vh] overflow-auto pr-1">
              {list.map((name) => {
                const isActive = selected === name;
                return (
                  <li key={name}>
                    <button
                      onClick={() => handleSolve(name)}
                      className={`w-full text-left px-3 py-2 rounded-xl border transition ${
                        isActive
                          ? "bg-gray-900 text-white border-gray-900"
                          : "bg-white hover:bg-gray-50 border-gray-200"
                      }`}
                    >
                      <span className="font-medium">{name}</span>
                    </button>
                  </li>
                );
              })}
              {!loadingList && list.length === 0 && !errorList && (
                <li className="text-sm text-gray-500">No puzzles found.</li>
              )}
            </ul>
          </div>

          {/* Future: Upload */}
          <div className="mt-4 bg-white rounded-2xl border border-gray-200 shadow-sm p-3">
            <h3 className="text-sm font-semibold mb-2">Upload (coming soon)</h3>
            <p className="text-xs text-gray-500">
              Later we’ll add <code>POST /api/solve/upload</code> for .jpnxml files.
            </p>
          </div>
        </aside>

        {/* Canvas */}
        <section className="md:col-span-8 lg:col-span-9">
          <div className="bg-white rounded-2xl border border-gray-200 shadow-sm p-4">
            <div className="flex items-center justify-between mb-3">
              <div>
                <h2 className="text-lg font-semibold">
                  {selected ? selected : "Select a puzzle"}
                </h2>
                {solving && (
                  <p className="text-sm text-gray-500">Solving…</p>
                )}
              </div>
              {solution && (
                <div className="text-xs text-gray-600">
                  Size: {solution.length} × {solution[0]?.length ?? 0}
                </div>
              )}
            </div>

            {errorSolve && (
              <div className="text-sm text-red-600 mb-3">{errorSolve}</div>
            )}

            {solution ? (
              <Grid data={solution} cellSize={cellSize} />
            ) : (
              <div className="h-64 flex items-center justify-center text-gray-400">
                <span className="text-sm">Nothing to show yet</span>
              </div>
            )}
          </div>
        </section>
      </main>

      <footer className="py-6 text-center text-xs text-gray-500">
        nonogram-ui · React + TypeScript · Vite
      </footer>
    </div>
  );
};

export default App;
