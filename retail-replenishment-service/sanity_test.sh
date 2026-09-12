#!/usr/bin/env bash
# Sanity test for retail-replenishment-service + Keycloak + RabbitMQ.
# Usage:
#   chmod +x sanity_test.sh
#   ./sanity_test.sh <keycloak-client-secret>
#
# Everything else (URLs, realm, client id) matches the local-dev defaults
# from application.yml and the Keycloak realm export. Edit the variables
# below if your setup differs.

set -euo pipefail

# ---- config ----------------------------------------------------------
KEYCLOAK_ISSUER="http://localhost:8080/realms/retail-replenishment"
API_BASE_URL="http://localhost:8087/api"
CLIENT_ID="retail-replenishment-agent"
CLIENT_SECRET="${1:-}"

if [ -z "$CLIENT_SECRET" ]; then
  echo "Usage: $0 <keycloak-client-secret>"
  echo "Find it in Keycloak: Clients -> retail-replenishment-agent -> Credentials"
  exit 1
fi

# Pretty-printer: use jq if present, else python3, else raw cat.
pp() {
  if command -v jq >/dev/null 2>&1; then
    jq .
  elif command -v python3 >/dev/null 2>&1; then
    python3 -m json.tool
  else
    cat
  fi
}

echo "== 1. Fetching access token from Keycloak =="
TOKEN_RESPONSE=$(curl -sS -X POST "${KEYCLOAK_ISSUER}/protocol/openid-connect/token" \
  -d grant_type=client_credentials \
  -d client_id="${CLIENT_ID}" \
  -d client_secret="${CLIENT_SECRET}")

echo "$TOKEN_RESPONSE" | pp

if command -v jq >/dev/null 2>&1; then
  ACCESS_TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.access_token')
else
  # Crude fallback extraction if jq isn't installed.
  ACCESS_TOKEN=$(echo "$TOKEN_RESPONSE" | python3 -c 'import json,sys; print(json.load(sys.stdin).get("access_token",""))' 2>/dev/null || true)
fi

if [ -z "${ACCESS_TOKEN:-}" ] || [ "$ACCESS_TOKEN" = "null" ]; then
  echo
  echo "FAILED: no access_token in response above. Check the client secret and that Keycloak is running on 8080."
  exit 1
fi
echo
echo "Got token (first 20 chars): ${ACCESS_TOKEN:0:20}..."
echo

echo "== 2. GET /inventory/low-stock =="
LOW_STOCK=$(curl -sS "${API_BASE_URL}/inventory/low-stock" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}")
echo "$LOW_STOCK" | pp
echo

if [ "$LOW_STOCK" = "[]" ]; then
  echo "WARNING: empty result. Either the seed data (V2) hasn't been loaded, or it's been truncated since."
fi

echo "== 3. GET /stores (to confirm master data + get real IDs for step 4) =="
STORES=$(curl -sS "${API_BASE_URL}/stores" -H "Authorization: Bearer ${ACCESS_TOKEN}")
echo "$STORES" | pp
echo

echo "== 4. GET /suppliers/availability for the Whole Milk product =="
# Resolves the productId for SKU-DRY-001 from the low-stock response above,
# so this works regardless of what auto-increment IDs your DB assigned.
MILK_PRODUCT_ID=$(echo "$LOW_STOCK" | python3 -c '
import json, sys
items = json.load(sys.stdin)
for i in items:
    if i.get("skuCode") == "SKU-DRY-001":
        print(i["productId"])
        break
' 2>/dev/null || true)

if [ -n "${MILK_PRODUCT_ID:-}" ]; then
  echo "Resolved SKU-DRY-001 -> productId=${MILK_PRODUCT_ID}"
  curl -sS "${API_BASE_URL}/suppliers/availability?productId=${MILK_PRODUCT_ID}" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" | pp
else
  echo "Could not resolve SKU-DRY-001's productId from the low-stock response — skipping."
fi
echo

echo "== Done. If steps 1-2 returned real data, the API + Keycloak wiring is good. =="
