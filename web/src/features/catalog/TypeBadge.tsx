export function TypeBadge({ type }: { type: string }) {
  return (
    <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-700 capitalize">{type}</span>
  )
}
