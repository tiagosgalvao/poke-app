const DEX_NUMBER_DIGITS = 3

export function dexNumber(id: number) {
  return `#${String(id).padStart(DEX_NUMBER_DIGITS, '0')}`
}

export function displayName(name: string) {
  return name
    .split('-')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ')
}
