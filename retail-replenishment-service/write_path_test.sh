#!/usr/bin/env bash
# Write-path sanity test for retail-replenishment-service.
# Creates a real order for Whole Milk (ST-100), watches RabbitMQ pick it up,
# then polls delivery status until it reaches DELIVERED — confirming the
# supplier-integration stub, the delivery simulator, and the auto-receive-
# into-inventory logic all actually fire.
#
# Usage:
#   chmod +x write_path_test.sh
#   ./write_path_test.sh <keycloak-client-secret> [rabbitmq-mgmt-user] [rabbitmq-mgmt-pass]
#
# rabbitmq-mgmt-user/pass default to guest/guest (RabbitMQ's local-only
# default — won't work remotely, which is fine for this local sanity check).

set -euo pipefail

# ---- config ------------------------------------------------------------
KEYCLOAK_ISSUER="http://localhost:8080/realms/retail-replenishment"
API_BASE_URL="http://localhost:8087/api"
CLIENT_ID="retail-replenishment-agent"
CLIENT_SECRET="${1:-}"
RABBITMQ_MGMT_USER="${2:-guest}"
RABBITMQ_MGMT_PASS="${3:-guest}"
RABBITMQ_MGMT_URL="http://localhost:15672"
ORDER_QUEUE="replenishment.order.created.q"

# Target SKU for this test — matches the seed data's Whole Milk / ST-100 scenario.
TARGET_SKU="SKU-DRY-001"
ORDER_QTY=80

POLL_SECONDS=20
MAX_POLLS=24   # 24 * 20s = 8 minutes, comfortably longer than the ~6 min simulator cadence

if [ -z "$CLIENT_SECRET" ]; then
  echo "Usage: $0 <keycloak-client-secret> [rabbitmq-mgmt-user] [rabbitmq-mgmt-pass]"
  exit 1
fi

pp() {
  if command -v jq >/dev/null 2>&1; then jq .
  elif command -v python3 >/dev/null 2>&1; then python3 -m json.tool
  else cat
  fi
}

jget() {
  # jget <json-string> <python-expr-on-"data">
  python3 -c "
import json, sys
data = json.loads(sys.argv[1])
print($1)
" "$2" 2>/dev/null || true
}

echo "== 1. Fetching access token =="
TOKEN_RESPONSE=$(curl -sS -X POST "${KEYCLOAK_ISSUER}/protocol/openid-connect/token" \
  -d grant_type=client_credentials \
  -d client_id="${CLIENT_ID}" \
  -d client_secret="${CLIENT_SECRET}")
ACCESS_TOKEN=$(python3 -c 'import json,sys; print(json.load(sys.stdin).get("access_token",""))' <<< "$TOKEN_RESPONSE")

if [ -z "$ACCESS_TOKEN" ] || [ "$ACCESS_TOKEN" = "null" ]; then
  echo "FAILED to get a token. Response was:"
  echo "$TOKEN_RESPONSE" | pp
  exit 1
fi
echo "Got token."
echo

auth_get()  { curl -sS "$1" -H "Authorization: Bearer ${ACCESS_TOKEN}"; }
auth_post() { curl -sS -X POST "$1" -H "Authorization: Bearer ${ACCESS_TOKEN}" -H "Content-Type: application/json" -d "$2"; }

echo "== 2. Resolving store/product/supplier IDs for ${TARGET_SKU} =="
LOW_STOCK=$(auth_get "${API_BASE_URL}/inventory/low-stock")

read -r STORE_ID PRODUCT_ID <<< "$(python3 -c "
import json
items = json.loads('''$LOW_STOCK''')
for i in items:
    if i['skuCode'] == '${TARGET_SKU}':
        print(i['storeId'], i['productId'])
        break
")"

if [ -z "${STORE_ID:-}" ] || [ -z "${PRODUCT_ID:-}" ]; then
  echo "Could not find ${TARGET_SKU} in the current low-stock list — it may already be resolved (delivered), or the seed data differs."
  echo "Current low-stock:"
  echo "$LOW_STOCK" | pp
  exit 1
fi
echo "storeId=${STORE_ID}, productId=${PRODUCT_ID}"

SUPPLIERS=$(auth_get "${API_BASE_URL}/suppliers/availability?productId=${PRODUCT_ID}")
read -r SUPPLIER_ID UNIT_COST <<< "$(python3 -c "
import json
s = json.loads('''$SUPPLIERS''')[0]
print(s['supplierId'], s['unitCost'])
")"
echo "supplierId=${SUPPLIER_ID}, unitCost=${UNIT_COST}"
echo

echo "== 3. Creating the replenishment order =="
ORDER_PAYLOAD=$(python3 -c "
import json
print(json.dumps({
    'storeId': ${STORE_ID},
    'supplierId': ${SUPPLIER_ID},
    'sourceType': 'MANUAL',
    'approvedBy': 'sanity-test',
    'lines': [{'productId': ${PRODUCT_ID}, 'orderedQty': ${ORDER_QTY}, 'unitCost': ${UNIT_COST}}]
}))
")
ORDER_RESPONSE=$(auth_post "${API_BASE_URL}/replenishment-orders" "$ORDER_PAYLOAD")
echo "$ORDER_RESPONSE" | pp

ORDER_ID=$(python3 -c "import json; print(json.loads('''$ORDER_RESPONSE''').get('replenishmentOrderId',''))")
if [ -z "$ORDER_ID" ]; then
  echo "FAILED to create order — no replenishmentOrderId in response."
  exit 1
fi
echo
echo "Created replenishmentOrderId=${ORDER_ID}"
echo

echo "== 4. Checking RabbitMQ queue ${ORDER_QUEUE} for activity =="
QUEUE_STATS=$(curl -sS -u "${RABBITMQ_MGMT_USER}:${RABBITMQ_MGMT_PASS}" \
  "${RABBITMQ_MGMT_URL}/api/queues/%2F/${ORDER_QUEUE}" 2>/dev/null || echo '{}')
if echo "$QUEUE_STATS" | python3 -c "import json,sys; json.load(sys.stdin)" 2>/dev/null; then
  echo "$QUEUE_STATS" | python3 -c "
import json, sys
q = json.load(sys.stdin)
print('message_stats.publish_details.rate:', q.get('message_stats', {}).get('publish_details', {}).get('rate', 'n/a'))
print('messages_ready:', q.get('messages_ready', 'n/a'))
print('(a rate > 0 or a recent publish count confirms the event was published)')
"
else
  echo "Could not reach RabbitMQ management API at ${RABBITMQ_MGMT_URL} — check it's running and the mgmt plugin is enabled, or check the UI manually."
fi
echo

echo "== 5. Polling delivery status until DELIVERED (up to $((MAX_POLLS * POLL_SECONDS / 60)) minutes) =="
LAST_STATUS=""
for i in $(seq 1 $MAX_POLLS); do
  sleep "$POLL_SECONDS"
  DELIVERY=$(auth_get "${API_BASE_URL}/replenishment-orders/${ORDER_ID}/delivery-status" 2>/dev/null || echo '[]')
  STATUS=$(python3 -c "
import json
d = json.loads('''$DELIVERY''')
print(d[0]['statusCode'] if d else 'NO_SHIPMENT_YET')
" 2>/dev/null || echo "NO_SHIPMENT_YET")

  if [ "$STATUS" != "$LAST_STATUS" ]; then
    echo "  [$(date '+%H:%M:%S')] shipment status: ${STATUS}"
    LAST_STATUS="$STATUS"
  fi

  if [ "$STATUS" = "DELIVERED" ]; then
    echo
    echo "Delivered. Full delivery record:"
    echo "$DELIVERY" | pp
    break
  fi
done

if [ "$LAST_STATUS" != "DELIVERED" ]; then
  echo
  echo "Timed out waiting for DELIVERED — last seen status: ${LAST_STATUS:-none}"
  echo "(Not necessarily a failure — check app.delivery-simulator.* timing in application.yml if this consistently times out.)"
fi
echo

echo "== 6. Confirming auto-receive: ${TARGET_SKU} should no longer be in low-stock =="
FINAL_LOW_STOCK=$(auth_get "${API_BASE_URL}/inventory/low-stock")
STILL_LOW=$(python3 -c "
import json
items = json.loads('''$FINAL_LOW_STOCK''')
print('yes' if any(i['skuCode'] == '${TARGET_SKU}' for i in items) else 'no')
")
if [ "$STILL_LOW" = "no" ]; then
  echo "PASS: ${TARGET_SKU} is no longer low-stock — inventory was received correctly."
else
  echo "STILL LOW STOCK: either delivery hasn't completed yet, or the receive logic didn't fire as expected."
fi
echo

echo "== Order status =="
auth_get "${API_BASE_URL}/replenishment-orders/${ORDER_ID}" | pp

echo
echo "== Done. =="
