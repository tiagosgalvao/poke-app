export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface Ability {
  name: string
  hidden: boolean
}

export interface Stat {
  name: string
  value: number
}

export interface EvolutionStage {
  stage: number
  id: number
  name: string
  evolvesFromId: number | null
  trigger: string | null
  spriteUrl: string | null
}

export interface PokemonSummary {
  id: number
  name: string
  spriteUrl: string | null
  category: string | null
  weightKg: number
  heightM: number
  types: string[]
  abilities: Ability[]
}

export interface PokemonDetail {
  id: number
  name: string
  spriteUrl: string | null
  imageUrl: string | null
  category: string | null
  weightKg: number
  heightM: number
  types: string[]
  abilities: Ability[]
  stats: Stat[]
  description: string | null
  evolution: EvolutionStage[]
}

export interface LocalPokemon {
  id: number
  name: string
  spriteUrl: string | null
  imageUrl: string | null
  category: string | null
  weightKg: number
  heightM: number
  types: string[]
  abilities: string[]
  localizedName: string | null
  region: string | null
  habitat: string | null
  tags: string[]
  notes: string | null
  version: number
  syncedAt: string
  updatedAt: string
}

export interface ProprietaryData {
  localizedName: string | null
  region: string | null
  habitat: string | null
  tags: string[]
  notes: string | null
}

export interface SyncSummary {
  created: number[]
  refreshed: number[]
  failed: number[]
}

export interface User {
  id: string
  username: string
  email: string
  createdAt: string
}

export interface AccessToken {
  accessToken: string
  tokenType: string
  expiresAt: string
}

export interface FieldError {
  field: string
  message: string
}

export interface ProblemDetail {
  title?: string
  status?: number
  detail?: string
  instance?: string
  fieldErrors?: FieldError[]
}
