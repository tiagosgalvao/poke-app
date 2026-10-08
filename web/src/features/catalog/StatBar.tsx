const MAX_BASE_STAT = 255
const PERCENT = 100

export function StatBar({ name, value }: { name: string; value: number }) {
  const width = Math.min(PERCENT, Math.round((value / MAX_BASE_STAT) * PERCENT))
  return (
    <div className="grid grid-cols-[8rem_3rem_1fr] items-center gap-2 text-sm">
      <span className="text-slate-600 capitalize">{name.replace('-', ' ')}</span>
      <span className="text-right font-semibold tabular-nums">{value}</span>
      <div
        role="progressbar"
        aria-label={name}
        aria-valuenow={value}
        aria-valuemin={0}
        aria-valuemax={MAX_BASE_STAT}
        className="h-2 overflow-hidden rounded-full bg-slate-100"
      >
        <div className="h-full rounded-full bg-sky-500" style={{ width: `${width}%` }} />
      </div>
    </div>
  )
}
