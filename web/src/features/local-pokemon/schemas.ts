import { z } from 'zod'

const MAX_LOCALIZED_NAME = 100
const MAX_PLACE = 50
const MAX_NOTES = 1000
const MAX_TAGS = 10
const MAX_SYNC_IDS = 50
const TAG = /^[A-Za-z0-9-]{1,30}$/
const ID_LIST = /^\s*\d+(\s*,\s*\d+)*\s*$/

export function parseTags(tags: string) {
  return tags
    .split(',')
    .map((tag) => tag.trim())
    .filter(Boolean)
}

export function parseIds(ids: string) {
  return ids.split(',').map((id) => Number(id.trim()))
}

export const editSchema = z.object({
  localizedName: z.string().max(MAX_LOCALIZED_NAME, `At most ${MAX_LOCALIZED_NAME} characters`),
  region: z.string().max(MAX_PLACE, `At most ${MAX_PLACE} characters`),
  habitat: z.string().max(MAX_PLACE, `At most ${MAX_PLACE} characters`),
  tags: z
    .string()
    .refine((tags) => parseTags(tags).every((tag) => TAG.test(tag)), 'Tags use letters, digits and hyphens (up to 30 each)')
    .refine((tags) => parseTags(tags).length <= MAX_TAGS, `At most ${MAX_TAGS} tags`),
  notes: z.string().max(MAX_NOTES, `At most ${MAX_NOTES} characters`),
})

export const syncSchema = z.object({
  ids: z
    .string()
    .regex(ID_LIST, 'Enter comma-separated numbers, e.g. 1, 4, 7')
    .refine((ids) => parseIds(ids).length <= MAX_SYNC_IDS, `At most ${MAX_SYNC_IDS} ids per sync`),
})

export type EditForm = z.infer<typeof editSchema>
export type SyncForm = z.infer<typeof syncSchema>
