import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { dexNumber, displayName } from './format'
import { Pagination } from './Pagination'

describe('format helpers', () => {
  it('pads national dex numbers and capitalizes hyphenated names', () => {
    expect(dexNumber(7)).toBe('#007')
    expect(dexNumber(1025)).toBe('#1025')
    expect(displayName('mr-mime')).toBe('Mr Mime')
  })
})

describe('Pagination', () => {
  it('renders nothing for a single page', () => {
    const { container } = render(<Pagination page={1} totalPages={1} onChange={() => {}} />)

    expect(container).toBeEmptyDOMElement()
  })

  it('disables the edges and reports page changes', async () => {
    const onChange = vi.fn()
    render(<Pagination page={1} totalPages={3} onChange={onChange} />)

    expect(screen.getByRole('button', { name: /previous/i })).toBeDisabled()
    await userEvent.click(screen.getByRole('button', { name: /next/i }))

    expect(onChange).toHaveBeenCalledWith(2)
  })
})
