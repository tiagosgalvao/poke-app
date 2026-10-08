import type { LocalPokemon, Page, PokemonDetail, PokemonSummary } from '../api/types'

export function summary(id: number, name: string, overrides: Partial<PokemonSummary> = {}): PokemonSummary {
  return {
    id,
    name,
    spriteUrl: `https://img/${id}.png`,
    category: 'Mouse Pokémon',
    weightKg: 6,
    heightM: 0.4,
    types: ['electric'],
    abilities: [
      { name: 'static', hidden: false },
      { name: 'lightning-rod', hidden: true },
    ],
    ...overrides,
  }
}

export function page<T>(content: T[], pageNumber = 0, totalElements = content.length, size = 20): Page<T> {
  return { content, page: pageNumber, size, totalElements, totalPages: Math.ceil(totalElements / size) }
}

export const pikachuDetail: PokemonDetail = {
  id: 25,
  name: 'pikachu',
  spriteUrl: 'https://img/25.png',
  imageUrl: 'https://img/art/25.png',
  category: 'Mouse Pokémon',
  weightKg: 6,
  heightM: 0.4,
  types: ['electric'],
  abilities: [
    { name: 'static', hidden: false },
    { name: 'lightning-rod', hidden: true },
  ],
  stats: [
    { name: 'hp', value: 35 },
    { name: 'speed', value: 90 },
  ],
  description: 'Possesses cheek sacs in which it stores electricity.',
  evolution: [
    { stage: 0, id: 172, name: 'pichu', evolvesFromId: null, trigger: null, spriteUrl: 'https://img/172.png' },
    { stage: 1, id: 25, name: 'pikachu', evolvesFromId: 172, trigger: 'level-up', spriteUrl: 'https://img/25.png' },
    { stage: 2, id: 26, name: 'raichu', evolvesFromId: 25, trigger: 'use-item: thunder-stone', spriteUrl: 'https://img/26.png' },
  ],
}

export function localPokemon(id: number, name: string, overrides: Partial<LocalPokemon> = {}): LocalPokemon {
  return {
    id,
    name,
    spriteUrl: `https://img/${id}.png`,
    imageUrl: `https://img/art/${id}.png`,
    category: 'Mouse Pokémon',
    weightKg: 6,
    heightM: 0.4,
    types: ['electric'],
    abilities: ['static'],
    localizedName: null,
    region: null,
    habitat: null,
    tags: [],
    notes: null,
    version: 0,
    syncedAt: '2026-10-08T10:00:00Z',
    updatedAt: '2026-10-08T10:00:00Z',
    ...overrides,
  }
}
