#!/usr/bin/env bash
# Repairs the retail-replenishment realm: creates the standard OIDC
# "profile" and "email" client scopes (missing because the realm was
# bootstrapped from a partial JSON import rather than Keycloak's
# "Create Realm" flow, which seeds these automatically) and assigns
# them as default scopes on retail-replenishment-dashboard.
#
# Usage:
#   chmod +x fix_keycloak_scopes.sh
#   ./fix_keycloak_scopes.sh <master-realm-admin-password>
#
# Assumes: Keycloak on http://localhost:8080, master realm admin user
# "binitadmin", target realm "retail-replenishment", target client
# "retail-replenishment-dashboard". Edit the variables below if any differ.

set -euo pipefail

KEYCLOAK_URL="http://localhost:8080"
MASTER_ADMIN_USER="binitadmin"
#MASTER_ADMIN_PASSWORD="${1:-}"
MASTER_ADMIN_PASSWORD="password"
TARGET_REALM="retail-replenishment"
TARGET_CLIENT_ID="retail-replenishment-dashboard"

if [ -z "$MASTER_ADMIN_PASSWORD" ]; then
  echo "Usage: $0 <master-realm-admin-password>"
  exit 1
fi

echo "== 1. Authenticating as master realm admin =="
ADMIN_TOKEN=$(curl -sS -X POST "${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token" \
  -d grant_type=password \
  -d client_id=admin-cli \
  -d username="${MASTER_ADMIN_USER}" \
  -d password="${MASTER_ADMIN_PASSWORD}" \
  | python3 -c 'import json,sys; print(json.load(sys.stdin)["access_token"])')

if [ -z "$ADMIN_TOKEN" ] || [ "$ADMIN_TOKEN" = "null" ]; then
  echo "FAILED to authenticate. Check the password and that Keycloak is on ${KEYCLOAK_URL}."
  exit 1
fi
echo "Authenticated."
echo

auth_curl() {
  curl -sS -H "Authorization: Bearer ${ADMIN_TOKEN}" -H "Content-Type: application/json" "$@"
}

# ---- helper: create a client scope if it doesn't already exist ---------
create_scope_if_missing() {
  local scope_name="$1"
  local existing
  existing=$(auth_curl "${KEYCLOAK_URL}/admin/realms/${TARGET_REALM}/client-scopes" \
    | python3 -c "
import json, sys
scopes = json.load(sys.stdin)
match = [s['id'] for s in scopes if s['name'] == '${scope_name}']
print(match[0] if match else '')
")
  if [ -n "$existing" ]; then
    echo "Scope '${scope_name}' already exists (id=${existing}) — skipping creation."
    echo "$existing"
    return
  fi

  local body
  body=$(python3 -c "
import json
print(json.dumps({
    'name': '${scope_name}',
    'protocol': 'openid-connect',
    'attributes': {
        'include.in.token.scope': 'true',
        'display.on.consent.screen': 'true'
    }
}))
")
  auth_curl -X POST "${KEYCLOAK_URL}/admin/realms/${TARGET_REALM}/client-scopes" -d "$body" >/dev/null

  auth_curl "${KEYCLOAK_URL}/admin/realms/${TARGET_REALM}/client-scopes" \
    | python3 -c "
import json, sys
scopes = json.load(sys.stdin)
match = [s['id'] for s in scopes if s['name'] == '${scope_name}']
print(match[0] if match else '')
"
}

# ---- helper: add one protocol mapper to a scope -------------------------
add_mapper() {
  local scope_id="$1" mapper_name="$2" user_attribute="$3" claim_name="$4" claim_type="${5:-String}"
  local body
  body=$(python3 -c "
import json
print(json.dumps({
    'name': '${mapper_name}',
    'protocol': 'openid-connect',
    'protocolMapper': 'oidc-usermodel-property-mapper',
    'consentRequired': False,
    'config': {
        'userinfo.token.claim': 'true',
        'user.attribute': '${user_attribute}',
        'id.token.claim': 'true',
        'access.token.claim': 'true',
        'claim.name': '${claim_name}',
        'jsonType.label': '${claim_type}'
    }
}))
")
  auth_curl -X POST "${KEYCLOAK_URL}/admin/realms/${TARGET_REALM}/client-scopes/${scope_id}/protocol-mappers/models" \
    -d "$body" >/dev/null
  echo "  + mapper ${mapper_name} (${user_attribute} -> ${claim_name})"
}

echo "== 2. Creating 'profile' scope + standard mappers =="
PROFILE_SCOPE_ID=$(create_scope_if_missing "profile")
add_mapper "$PROFILE_SCOPE_ID" "given name"   "firstName" "given_name"
add_mapper "$PROFILE_SCOPE_ID" "family name"  "lastName"  "family_name"
add_mapper "$PROFILE_SCOPE_ID" "username"     "username"  "preferred_username"
echo

echo "== 3. Creating 'email' scope + standard mappers =="
EMAIL_SCOPE_ID=$(create_scope_if_missing "email")
add_mapper "$EMAIL_SCOPE_ID" "email"          "email"         "email"
add_mapper "$EMAIL_SCOPE_ID" "email verified" "emailVerified" "email_verified" "boolean"
echo

echo "== 4. Resolving ${TARGET_CLIENT_ID}'s internal client id =="
CLIENT_UUID=$(auth_curl "${KEYCLOAK_URL}/admin/realms/${TARGET_REALM}/clients?clientId=${TARGET_CLIENT_ID}" \
  | python3 -c 'import json,sys; print(json.load(sys.stdin)[0]["id"])')
echo "client internal id: ${CLIENT_UUID}"
echo

echo "== 5. Assigning 'profile' and 'email' as DEFAULT scopes on ${TARGET_CLIENT_ID} =="
auth_curl -X PUT "${KEYCLOAK_URL}/admin/realms/${TARGET_REALM}/clients/${CLIENT_UUID}/default-client-scopes/${PROFILE_SCOPE_ID}"
auth_curl -X PUT "${KEYCLOAK_URL}/admin/realms/${TARGET_REALM}/clients/${CLIENT_UUID}/default-client-scopes/${EMAIL_SCOPE_ID}"
echo "Done."
echo

echo "== 6. Verifying =="
auth_curl "${KEYCLOAK_URL}/admin/realms/${TARGET_REALM}/clients/${CLIENT_UUID}/default-client-scopes" \
  | python3 -c "
import json, sys
scopes = json.load(sys.stdin)
print('Default scopes now assigned:', [s['name'] for s in scopes])
"

echo
echo "== Fix applied. Close and reopen the incognito window, go to http://localhost:4200, and log in again. =="
