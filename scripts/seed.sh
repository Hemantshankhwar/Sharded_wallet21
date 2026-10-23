#!/usr/bin/env bash
# Seeds demo users, wallets and transfers through the API as the admin.
# Usage: ADMIN_EMAIL=... ADMIN_PASSWORD=... [BASE_URL=http://localhost:3001] [SEED_USER_PASSWORD=...] scripts/seed.sh
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:3001}"
: "${ADMIN_EMAIL:?set ADMIN_EMAIL}"
: "${ADMIN_PASSWORD:?set ADMIN_PASSWORD}"
USER_PASSWORD="${SEED_USER_PASSWORD:-Demo@1234}"

json() { python3 -c "import sys,json;print(json.load(sys.stdin)$1)"; }

TOKEN=$(curl -sf -X POST "$BASE_URL/auth/login" -H 'Content-Type: application/json' \
  -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}" | json "['token']")

api() { # method path [body]
  curl -sf -X "$1" "$BASE_URL$2" -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
    -H "Idempotency-Key: $(uuidgen)" ${3:+-d "$3"}
}

declare -a USERS=("Aarav Sharma:aarav@example.com" "Priya Nair:priya@example.com" "Rohan Mehta:rohan@example.com"
  "Sneha Iyer:sneha@example.com" "Vikram Singh:vikram@example.com" "Ananya Das:ananya@example.com")

declare -a WALLETS=()
for entry in "${USERS[@]}"; do
  name="${entry%%:*}"; email="${entry##*:}"
  uid=$(api POST /admin/users "{\"name\":\"$name\",\"email\":\"$email\",\"password\":\"$USER_PASSWORD\",\"role\":\"USER\"}" | json "['id']")
  wid=$(api POST /wallets "{\"userId\":$uid}" | json "['id']")
  api POST "/wallets/$wid/credit" '{"amount":10000}' >/dev/null
  WALLETS+=("$wid")
  echo "created $name ($email) user=$uid wallet=$wid"
done

transfer() { # fromIdx toIdx amount description
  api POST /transactions "{\"fromWalletId\":${WALLETS[$1]},\"toWalletId\":${WALLETS[$2]},\"amount\":$3,\"description\":\"$4\"}" >/dev/null
  echo "transfer $3: wallet ${WALLETS[$1]} -> ${WALLETS[$2]}"
}
transfer 0 1 1500 "Rent share"
transfer 1 2 400 "Dinner"
transfer 2 3 250 "Movie tickets"
transfer 3 4 1200 "Trip contribution"
transfer 4 0 300 "Groceries"
transfer 5 0 900 "Loan repayment"

echo "Done. Seeded users log in with password: $USER_PASSWORD"
