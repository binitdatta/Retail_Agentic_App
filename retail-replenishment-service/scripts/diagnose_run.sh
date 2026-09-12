#!/usr/bin/env bash
# Diagnoses "Could not load this run" by calling the exact four endpoints
# the Angular run-detail page calls, showing the real HTTP status and body
# for each — instead of guessing which one is failing.
#
# Usage: ./diagnose_run.sh <keycloak-client-secret> <run-id>

set -euo pipefail

CLIENT_SECRET="${1:-}"
RUN_ID="${2:-1}"
KEYCLOAK_ISSUER="http://localhost:8080/realms/retail-replenishment"
API_BASE_URL="http://localhost:8087/api"

if [ -z "$CLIENT_SECRET" ]; then
  echo "Usage: $0 <keycloak-client-secret> <run-id>"
  exit 1
fi

echo "== Fetching token =="
TOKEN=$(curl -sS -X POST "${KEYCLOAK_ISSUER}/protocol/openid-connect/token" \
  -d grant_type=client_credentials \
  -d client_id=retail-replenishment-agent \
  -d client_secret="${CLIENT_SECRET}" \
  | python3 -c 'import json,sys; print(json.load(sys.stdin)["access_token"])')

if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
  echo "FAILED to get a token — check the client secret."
  exit 1
fi
echo "Got token."
echo

check() {
  local name="$1" url="$2"
  echo "== ${name}: GET ${url} =="
  local status
  status=$(curl -sS -o /tmp/resp_body.txt -w "%{http_code}" "${url}" -H "Authorization: Bearer ${TOKEN}")
  echo "Status: ${status}"
  echo "Body:"
  cat /tmp/resp_body.txt
  echo
  echo
}

check "1. Run info"   "${API_BASE_URL}/agent-runs/${RUN_ID}"
check "2. Decisions"  "${API_BASE_URL}/agent-runs/${RUN_ID}/decisions"
check "3. LLM calls"  "${API_BASE_URL}/agent-runs/${RUN_ID}/llm-calls"
check "4. LLM cost"   "${API_BASE_URL}/agent-runs/${RUN_ID}/llm-cost"

echo "== Done. Whichever call above did NOT show Status: 200 is the actual failure. =="