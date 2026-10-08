#!/usr/bin/env bash
# Runs the demo flow against a running stack, through the web container's /api proxy.
# Usage: scripts/smoke-test.sh [base-url]   (default http://localhost:3000; needs curl and jq)
set -uo pipefail

BASE="${1:-http://localhost:3000}"
API="$BASE/api/v1"
USER="smoke-$RANDOM$RANDOM"
PASSWORD='Smoke-Test-123!'
failures=0
body=$(mktemp)
trap 'rm -f "$body"' EXIT

# call <method> <path> [json] [token] -> sets $status, writes the body to $body
call() {
  local args=(-s -o "$body" -w '%{http_code}' -X "$1" -H 'Content-Type: application/json')
  [ -n "${3:-}" ] && args+=(-d "$3")
  [ -n "${4:-}" ] && args+=(-H "Authorization: Bearer $4")
  status=$(curl "${args[@]}" "$2")
}

expect() {
  local name=$1 want=$2 filter=${3:-}
  if [ "$status" != "$want" ]; then
    echo "FAIL $name: expected $want, got $status: $(head -c 300 "$body")"; failures=$((failures + 1)); return
  fi
  if [ -n "$filter" ] && ! jq -e "$filter" "$body" > /dev/null 2>&1; then
    echo "FAIL $name: body does not satisfy $filter: $(head -c 300 "$body")"; failures=$((failures + 1)); return
  fi
  echo "ok   $name"
}

call GET "$BASE/";                                   expect "web: SPA index" 200
call GET "$BASE/my-pokedex";                         expect "web: deep link falls back to the SPA" 200

call GET "$API/pokemon?page=0&size=20";              expect "US01 catalog page" 200 '.content | length == 20 and (.[0].abilities | length > 0)'
call GET "$API/pokemon?page=0&size=20";              expect "US01 catalog page again (cached)" 200 '.totalElements > 1000'
call GET "$API/pokemon/eevee";                       expect "US02 detail with evolution chain" 200 '(.stats | length == 6) and .description != null and ([.evolution[] | select(.stage == 1)] | length == 8)'
call GET "$API/pokemon/missingno";                   expect "US02 unknown Pokemon is a 404 ProblemDetail" 404 '.status == 404 and .detail != null'
call GET "$API/pokemon?size=500";                    expect "page size above 50 is a 400" 400

call GET "$API/local-pokemon";                       expect "local store is seeded" 200 '.totalElements >= 20'
call POST "$API/local-pokemon/sync" '{"ids":[1]}';   expect "sync without a token is a 401" 401

call POST "$API/auth/register" "{\"username\":\"$USER\",\"email\":\"$USER@example.com\",\"password\":\"$PASSWORD\"}"
expect "register" 201
call POST "$API/auth/register" "{\"username\":\"$USER\",\"email\":\"$USER@example.com\",\"password\":\"$PASSWORD\"}"
expect "duplicate registration is a 409" 409
call POST "$API/auth/login" "{\"username\":\"$USER\",\"password\":\"wrong-password\"}"
expect "wrong password is a 401" 401
call POST "$API/auth/login" '{"username":"ash","password":"Pikachu123!"}'
expect "seeded user logs in" 200 '.accessToken != null'
token=$(jq -r .accessToken "$body")

call POST "$API/local-pokemon/sync" '{"fromId":1,"toId":10}' "$token"
expect "US03 sync 1-10" 200 '(.created | length) + (.refreshed | length) + (.failed | length) == 10'
call POST "$API/local-pokemon" '{"idOrName":"pikachu"}' "$token"
expect "US03 importing a stored Pokemon is a 409" 409

call GET "$API/local-pokemon/1";                     expect "US04 read Bulbasaur" 200 '.version >= 0'
version=$(jq -r .version "$body")
call PATCH "$API/local-pokemon/1" "{\"version\":$version,\"localizedName\":\"  \"}" "$token"
expect "US04 blank localized name is a 400 with fieldErrors" 400 '.fieldErrors[0].field == "localizedName"'
call PATCH "$API/local-pokemon/1" '{"version":0,' "$token"
expect "US04 malformed JSON is a 400" 400
call PATCH "$API/local-pokemon/1" "{\"version\":$version,\"region\":\"Kanto\",\"tags\":[\"starter\",\"grass\"]}" "$token"
expect "US04 patch proprietary data" 200 ".version == $version + 1 and (.tags | index(\"grass\"))"
call PATCH "$API/local-pokemon/1" "{\"version\":$version,\"region\":\"Johto\"}" "$token"
expect "US04 stale version is a 409" 409
call PATCH "$API/local-pokemon/99999" '{"version":0,"region":"Kanto"}' "$token"
expect "US04 missing record is a 404" 404

call POST "$API/local-pokemon" '{"idOrName":"ditto"}' "$token"
expect "import Ditto" 201 '.name == "ditto"'
call DELETE "$API/local-pokemon/132" "" "$token"; expect "delete Ditto" 204
call GET "$API/local-pokemon/132";                expect "deleted Ditto is gone" 404

echo
if [ "$failures" -eq 0 ]; then echo "smoke test passed"; else echo "$failures check(s) failed"; exit 1; fi
