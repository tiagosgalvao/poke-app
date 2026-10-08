import { secondaryButton } from './buttonStyles'

interface PaginationProps {
  page: number
  totalPages: number
  onChange: (page: number) => void
}

export function Pagination({ page, totalPages, onChange }: PaginationProps) {
  if (totalPages <= 1) {
    return null
  }
  return (
    <nav aria-label="Pagination" className="flex items-center justify-center gap-3 py-6">
      <button type="button" className={secondaryButton} disabled={page <= 1} onClick={() => onChange(page - 1)}>
        Previous
      </button>
      <span className="text-sm text-slate-600">
        Page {page} of {totalPages}
      </span>
      <button type="button" className={secondaryButton} disabled={page >= totalPages} onClick={() => onChange(page + 1)}>
        Next
      </button>
    </nav>
  )
}
