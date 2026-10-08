import { secondaryButton } from './buttonStyles'

interface ErrorStateProps {
  message: string
  onRetry: () => void
}

export function ErrorState({ message, onRetry }: ErrorStateProps) {
  return (
    <div className="flex flex-col items-start gap-3 rounded-lg border border-red-200 bg-red-50 p-4">
      <p role="alert" className="text-sm text-red-700">
        {message}
      </p>
      <button type="button" className={secondaryButton} onClick={onRetry}>
        Try again
      </button>
    </div>
  )
}
