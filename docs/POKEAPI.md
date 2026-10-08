# PokeAPI reference (what we use and how)

Upstream: **https://pokeapi.co/api/v2**. Official docs: https://pokeapi.co/docs/v2

This page documents only the parts of PokeAPI the Poke App depends on: the endpoints, the fields we read, how they map to our domain, and the quirks we must handle. Examples are trimmed real responses, captured 2026-10-07.

- [1. General behaviour](#1-general-behaviour)
- [2. Endpoints we call](#2-endpoints-we-call)
- [3. `GET /pokemon` (list)](#3-get-pokemon-list)
- [4. `GET /pokemon/{id or name}`](#4-get-pokemonid-or-name)
- [5. `GET /pokemon-species/{id or name}`](#5-get-pokemon-speciesid-or-name)
- [6. `GET /evolution-chain/{id}`](#6-get-evolution-chainid)
- [7. Mapping to our API](#7-mapping-to-our-api)
- [8. Quirks & defensive handling](#8-quirks--defensive-handling)
- [9. Test fixtures](#9-test-fixtures)

## 1. General behaviour

| Aspect | Behaviour |
|---|---|
| Auth | None. The API is public and read-only, and only `GET` is supported. |
| Format | `application/json; charset=utf-8` |
| Caching upstream | Served through Cloudflare with `cache-control: public, max-age=86400`. The data is effectively static. |
| Fair use | No hard rate-limit headers, but the docs ask consumers to **cache locally**. That justifies our Redis cache (24 h TTL, matching upstream `max-age`). |
| Not found | `404` with a small JSON body `{"status":404,"message":"Not Found"}`. It is not RFC 9457, so we only use the status code. |
| Invalid id (`-1`, `0`, huge) | `404` |
| Lookup key | Numeric id or lowercase name (`/pokemon/25`, `/pokemon/pikachu`). We normalize names to lowercase and trim them before calling. |
| Trailing slash | URLs embedded in responses end with `/`. Some paths without the slash may answer with a redirect, so the HTTP client must **follow redirects**. We prefer the URLs exactly as PokeAPI returns them. |
| Root `GET /` | An index of about 50 resource types (`ability`, `pokemon`, `pokemon-species`, `evolution-chain`, …) |

**Named resource reference:** most links between resources look like `{ "name": "...", "url": "https://pokeapi.co/api/v2/<resource>/<id>/" }`. The id is the last path segment of `url`.

## 2. Endpoints we call

| Our feature | Upstream call(s) | Cache name |
|---|---|---|
| US01 list page | `GET /pokemon?offset&limit`, then for each result `GET /pokemon/{id}` and `GET /pokemon-species/{id}` | `pokemon-page`, `pokemon`, `species` |
| US02 detail | `GET /pokemon/{idOrName}`, then `GET {species.url}`, then `GET {species.evolution_chain.url}` | `pokemon`, `species`, `evolution-chain` |
| US03 sync / import | `GET /pokemon/{id}` and `GET /pokemon-species/{id}`, reusing the same cached `PokeApiClient` methods | `pokemon`, `species` |

## 3. `GET /pokemon` (list)

`GET /pokemon?offset=0&limit=3`

```json
{
  "count": 1351,
  "next": "https://pokeapi.co/api/v2/pokemon?offset=3&limit=3",
  "previous": null,
  "results": [
    { "name": "bulbasaur", "url": "https://pokeapi.co/api/v2/pokemon/1/" },
    { "name": "ivysaur",   "url": "https://pokeapi.co/api/v2/pokemon/2/" },
    { "name": "venusaur",  "url": "https://pokeapi.co/api/v2/pokemon/3/" }
  ]
}
```

- **It returns only name and URL.** Everything US01 needs (sprite, category, weight, abilities) requires one extra call per item, to `/pokemon/{id}` and `/pokemon-species/{id}`.
- `count` is **1351**. That includes the 1025 species (ids 1–1025) plus alternate forms and megas with **ids ≥ 10001** (e.g. `10001 deoxys-attack`, up to `10326`).
- The last page is short (e.g. `offset=1340&limit=20` → 11 results, `next: null`). An offset past the end returns an empty `results`.
- **Our page mapping:** `offset = page * size`, `limit = size`. `totalElements = count`, and `totalPages = ceil(count / size)`.

## 4. `GET /pokemon/{id or name}`

`GET /pokemon/25`. The response has about 20 top-level keys; we read only these:

```json
{
  "id": 25,
  "name": "pikachu",
  "height": 4,
  "weight": 60,
  "abilities": [
    { "is_hidden": false, "slot": 1, "ability": { "name": "static", "url": ".../ability/9/" } },
    { "is_hidden": true,  "slot": 3, "ability": { "name": "lightning-rod", "url": ".../ability/31/" } }
  ],
  "types": [ { "slot": 1, "type": { "name": "electric", "url": ".../type/13/" } } ],
  "stats": [
    { "base_stat": 35, "effort": 0, "stat": { "name": "hp" } },
    { "base_stat": 55, "effort": 0, "stat": { "name": "attack" } },
    { "base_stat": 40, "effort": 0, "stat": { "name": "defense" } },
    { "base_stat": 50, "effort": 0, "stat": { "name": "special-attack" } },
    { "base_stat": 50, "effort": 0, "stat": { "name": "special-defense" } },
    { "base_stat": 90, "effort": 0, "stat": { "name": "speed" } }
  ],
  "sprites": {
    "front_default": "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/25.png",
    "other": {
      "official-artwork": {
        "front_default": "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/25.png"
      }
    }
  },
  "species": { "name": "pikachu", "url": "https://pokeapi.co/api/v2/pokemon-species/25/" }
}
```

| Field | Notes |
|---|---|
| `height` | **Decimetres** (4 → 0.4 m) |
| `weight` | **Hectograms** (60 → 6.0 kg). This is the spec's "mass". We keep the raw value in the domain and expose `weightKg` in the API. |
| `abilities[]` | This is the spec's "skills". Keep `is_hidden` so the UI can mark hidden abilities. Sort by `slot`. |
| `stats[]` | Always these six names: `hp`, `attack`, `defense`, `special-attack`, `special-defense`, `speed`. |
| `sprites.front_default` | Small sprite for the US01 list. **May be `null`** for some forms. |
| `sprites.other.official-artwork.front_default` | Large image for the US02 detail. Fall back to `front_default` when it is `null`. |
| `species.url` | **Always use this to reach the species.** For forms (id ≥ 10001) the species id differs, e.g. `deoxys-attack` (10001) → species 386. |
| Ignored | `moves`, `game_indices`, `held_items`, `cries`, `sprites.versions`, `past_*`. These are large payloads we don't need. Our DTOs ignore unknown properties. |

## 5. `GET /pokemon-species/{id or name}`

`GET /pokemon-species/25`, trimmed:

```json
{
  "id": 25,
  "name": "pikachu",
  "genera": [
    { "genus": "Mouse Pokémon", "language": { "name": "en" } }
  ],
  "flavor_text_entries": [
    { "flavor_text": "When several of\nthese POKéMON\ngather, their\felectricity could\nbuild and cause\nlightning storms.",
      "language": { "name": "en" }, "version": { "name": "red" } },
    { "flavor_text": "Possesses cheek sacs in which it stores electricity. This clever\nforest-dweller roasts tough berries with an electric shock before\nconsuming them.",
      "language": { "name": "en" }, "version": { "name": "legends-arceus" } }
  ],
  "names": [
    { "name": "Pikachu",    "language": { "name": "en" } },
    { "name": "ピカチュウ", "language": { "name": "ja" } }
  ],
  "habitat":    { "name": "forest",       "url": ".../pokemon-habitat/2/" },
  "generation": { "name": "generation-i", "url": ".../generation/1/" },
  "evolution_chain": { "url": "https://pokeapi.co/api/v2/evolution-chain/10/" },
  "evolves_from_species": { "name": "pichu", "url": ".../pokemon-species/172/" },
  "is_legendary": false,
  "is_mythical": false
}
```

| Field | Our use |
|---|---|
| `genera[lang=en].genus` | **Category** for US01/US02 (e.g. "Mouse Pokémon"). Upstream text is passed through unchanged. |
| `flavor_text_entries[lang=en]` | **Description** for US02. Pikachu has 33 English entries across game versions. We take the **latest English entry**, i.e. the last one in the array. The client keeps only that entry (and the English genus) before caching. |
| `evolution_chain.url` | Next call for US02 |
| `names[]`, `habitat`, `generation` | **Optional seed values for proprietary fields on import** (US03): `localizedName` from `ja`, `habitat` from `habitat.name`, `region` derived from `generation`. The user can then edit them freely. |
| `is_legendary`, `is_mythical` | Candidates for default `tags` on import (`legendary`, `mythical`) |

## 6. `GET /evolution-chain/{id}`

`GET /evolution-chain/67` (Eevee). This is a **recursive tree**, not a list:

```json
{
  "id": 67,
  "baby_trigger_item": null,
  "chain": {
    "is_baby": false,
    "species": { "name": "eevee", "url": ".../pokemon-species/133/" },
    "evolution_details": [],
    "evolves_to": [
      { "species": { "name": "vaporeon", "url": ".../pokemon-species/134/" },
        "evolution_details": [ { "trigger": { "name": "use-item" }, "item": { "name": "water-stone" }, "min_level": null } ],
        "evolves_to": [] },
      { "species": { "name": "jolteon",  "url": ".../pokemon-species/135/" }, "evolves_to": [] }
    ]
  }
}
```

The full Eevee chain has eight branches: vaporeon, jolteon, flareon, espeon, umbreon, leafeon, glaceon and sylveon.

We flatten it with a breadth-first walk, so stages come out in stage order:

```json
[
  { "stage": 0, "id": 133, "name": "eevee",    "evolvesFromId": null, "trigger": null },
  { "stage": 1, "id": 134, "name": "vaporeon", "evolvesFromId": 133,  "trigger": "use-item: water-stone" },
  { "stage": 1, "id": 135, "name": "jolteon",  "evolvesFromId": 133,  "trigger": "use-item: thunder-stone" }
]
```

- The species id comes from the last segment of `species.url`. The sprite URL is built as `.../sprites/pokemon/{id}.png`, so no extra call per stage is needed.
- Siblings at the same `stage` are **branches**. The UI renders them side by side.
- `evolution_details` can hold several entries, one per game generation. We use the first one and summarize it as `trigger` plus `min_level` or `item`.

## 7. Mapping to our API

| Our field | US | Source |
|---|---|---|
| `id`, `name` | 01, 02 | `pokemon.id`, `pokemon.name` |
| `spriteUrl` | 01, 02 | `pokemon.sprites.front_default` (also copied into the local Pokedex) |
| `imageUrl` | 02 | `pokemon.sprites.other.official-artwork.front_default`, falling back to `front_default` |
| `category` | 01, 02 | `species.genera[en].genus` |
| `weightKg`, `heightM` | 01, 02 | `pokemon.weight` (hg) ÷ 10, `pokemon.height` (dm) ÷ 10. The domain keeps the raw units (`weightHectograms`, `heightDecimetres`), and `PokemonResponses` in the controller layer converts them. |
| `abilities[] {name, hidden}` | 01, 02 | `pokemon.abilities[]` sorted by `slot` |
| `types[]` | 01, 02 | `pokemon.types[]` sorted by `slot` |
| `stats[] {name, value}` | 02 | `pokemon.stats[]` |
| `description` | 02 | Latest `species.flavor_text_entries[en]`, normalized |
| `evolution[]` | 02 | Flattened `evolution-chain` |

## 8. Quirks & defensive handling

| Quirk | Handling |
|---|---|
| Flavor text contains `\n`, `\f` (form feed) and soft hyphens (`\u00ad`), and older entries are ALL CAPS ("POKéMON") | Replace `\f` and `\n` with spaces, drop `\u00ad`, collapse whitespace, and prefer the latest entry, which is mixed case |
| No English `genus` or flavor text (rare forms) | `category` / `description` = `null`; the UI shows "Unknown" |
| `sprites.*` may be `null` | Nullable in DTOs; the UI shows a placeholder image |
| 404 body is an ad-hoc JSON | `PokeApiClient` maps the status to an empty result without reading the body, and `CatalogService` turns that into `PokemonNotFoundException` (404) |
| Timeouts, 5xx, connection errors, malformed responses | Map to `ExternalServiceUnavailableException` (or `MalformedPokeApiResponseException`) → our **503**. Use connect timeout 2 s and read timeout 5 s, with no retries on the request path. |
| Large payloads (`/pokemon/{id}` is about 300 KB because of `moves`) | DTOs declare only the fields we use, and species responses are cut down to the English genus and latest English flavor text before caching. Measured in Redis, a Pokemon (1.2 KB), its species (0.5 KB) and its chain (0.9 KB) take about 2.6 KB together, against about 350 KB of raw upstream JSON. |
| Forms (id ≥ 10001) | Always follow `species.url`; never assume species id = pokemon id |
| One list page = 1 + 2 × size upstream calls | Parallelize on virtual threads; cache per `pokemon` and `species` so neighbouring pages reuse entries; cap `size` at 50 |
| Names | Lowercase and trim; reject anything outside `[a-z0-9-]` with 400 before calling upstream |

## 9. Test fixtures

WireMock fixtures live in `api/src/test/resources/pokeapi/`. They are real responses trimmed to the fields the client reads:

| File | Purpose |
|---|---|
| `pokemon-list-offset24-limit2.json` | A list page (pikachu, raichu) for mapping and pagination maths (`count` 1351) |
| `pokemon-25.json`, `pokemon-species-25.json` | Pikachu: sprite, artwork, stats, hidden ability, English and Japanese genus, oldest and latest English flavor text |
| `pokemon-26.json`, `pokemon-species-26.json` | Raichu, the second entry of the list page |
| `pokemon-133.json`, `pokemon-species-133.json`, `evolution-chain-67.json` | Eevee with its branching chain |
| `evolution-chain-10.json` | Pikachu's linear chain (pichu → pikachu → raichu) |
| `pokemon-10001.json`, `pokemon-species-386.json` | An alternate form (deoxys-attack) whose species id differs |
| *inline stubs* | 404 (`{"status":404,"message":"Not Found"}`), 500, and a delayed response for the read timeout |

To refresh a fixture, re-download it and keep only the fields listed in sections 4–6, e.g.:

```bash
curl -sL https://pokeapi.co/api/v2/pokemon/25/ | jq '{id,name,height,weight,abilities,types,stats,sprites:{front_default:.sprites.front_default,other:{"official-artwork":{front_default:.sprites.other["official-artwork"].front_default}}},species}' > pokemon-25.json
```
