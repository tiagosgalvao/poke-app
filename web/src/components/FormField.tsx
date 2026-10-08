import { useId, type InputHTMLAttributes } from 'react'

interface FormFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string
  error?: string
}

export function FormField({ label, error, ...input }: FormFieldProps) {
  const id = useId()
  const errorId = `${id}-error`
  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={id} className="text-sm font-medium text-slate-700">
        {label}
      </label>
      <input
        id={id}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId : undefined}
        className="rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-sky-500 focus:ring-2 focus:ring-sky-200 focus:outline-none aria-invalid:border-red-500"
        {...input}
      />
      {error && (
        <p id={errorId} className="text-sm text-red-600">
          {error}
        </p>
      )}
    </div>
  )
}
