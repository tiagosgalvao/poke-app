import { Link } from 'react-router'
import type { EvolutionStage } from '../../api/types'
import { displayName } from '../../components/format'

function byStage(stages: EvolutionStage[]) {
  const grouped = new Map<number, EvolutionStage[]>()
  stages.forEach((stage) => grouped.set(stage.stage, [...(grouped.get(stage.stage) ?? []), stage]))
  return [...grouped.entries()].sort(([a], [b]) => a - b).map(([, members]) => members)
}

export function EvolutionChain({ stages, currentId }: { stages: EvolutionStage[]; currentId: number }) {
  if (stages.length <= 1) {
    return <p className="text-sm text-slate-500">This Pokemon does not evolve.</p>
  }
  return (
    <ol aria-label="Evolution chain" className="flex flex-wrap items-start gap-4">
      {byStage(stages).map((members) => (
        <li key={members[0].stage} className="flex flex-col gap-2">
          {members.map((stage) => (
            <Link
              key={stage.id}
              to={`/pokemon/${stage.name}`}
              aria-current={stage.id === currentId ? 'page' : undefined}
              className="flex items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm hover:border-sky-300 aria-[current=page]:border-sky-500 aria-[current=page]:bg-sky-50"
            >
              {stage.spriteUrl && <img src={stage.spriteUrl} alt="" width={40} height={40} className="h-10 w-10" />}
              <span className="flex flex-col">
                <span className="font-medium">{displayName(stage.name)}</span>
                {stage.trigger && <span className="text-xs text-slate-500">{stage.trigger}</span>}
              </span>
            </Link>
          ))}
        </li>
      ))}
    </ol>
  )
}
