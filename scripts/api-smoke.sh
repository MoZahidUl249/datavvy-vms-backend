#!/usr/bin/env bash
# Manual API check (course step 3): create → list → get → update → duplicate → delete.
# Needs the backend on :8080 with the local profile (demo users). Requires curl; jq optional.
set -u
BASE="${BASE:-http://localhost:8080/api/v1}"
NUMBER="${NUMBER:-RAJ-KA 11-$(( RANDOM % 9000 + 1000 ))}"
pass=0; fail=0

check() { # label expected actual
  if [ "$2" = "$3" ]; then echo "  PASS  $1 ($3)"; pass=$((pass+1)); else echo "  FAIL  $1: expected $2, got $3"; fail=$((fail+1)); fi
}
field() { # json key  (tiny extractor so jq isn't required)
  python3 -c "import json,sys; d=json.load(sys.stdin); print(d.get('$1',''))" 2>/dev/null
}
call() { # method path [body] -> sets CODE and BODY
  local out
  if [ $# -ge 3 ]; then
    out=$(curl -s -w '\n%{http_code}' -X "$1" "$BASE$2" -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d "$3")
  else
    out=$(curl -s -w '\n%{http_code}' -X "$1" "$BASE$2" -H "Authorization: Bearer $TOKEN")
  fi
  CODE=$(echo "$out" | tail -n1); BODY=$(echo "$out" | sed '$d')
}

echo "== Login"
TOKEN=$(curl -s -X POST "$BASE/auth/login" -H 'Content-Type: application/json' \
  -d '{"email":"manager@datavvy.com","password":"Manager@12345"}' | field accessToken)
[ -n "$TOKEN" ] || { echo "  FAIL  login (is the backend running with the local profile?)"; exit 1; }
echo "  PASS  login"

body() { printf '{"vehicleNumber":"%s","company":"Datavvy Logistics","type":"Microbus","brand":"Toyota","model":"%s","status":"%s"%s}' "$1" "$2" "$3" "${4:-}"; }

echo "== Create $NUMBER"
call POST /vehicles "$(body "$NUMBER" Hiace ACTIVE)"
check "create → 201" 201 "$CODE"; ID=$(echo "$BODY" | field id); VERSION=$(echo "$BODY" | field version)
echo "     $BODY"

echo "== List"
call GET "/vehicles?size=100"
check "list → 200" 200 "$CODE"
echo "     totalElements=$(echo "$BODY" | field totalElements)"

echo "== Get $ID"
call GET "/vehicles/$ID"
check "get → 200" 200 "$CODE"

echo "== Update $ID (status MAINTENANCE)"
call PUT "/vehicles/$ID" "$(body "$NUMBER" 'Hiace GL' MAINTENANCE ",\"version\":$VERSION")"
check "update → 200" 200 "$CODE"
check "status changed" MAINTENANCE "$(echo "$BODY" | field status)"

echo "== Duplicate vehicle number (same number, different spelling)"
LOOSE=$(echo "$NUMBER" | tr 'A-Z-' 'a-z ')
call POST /vehicles "$(body "$LOOSE" Hiace ACTIVE)"
check "duplicate → 409" 409 "$CODE"
check "code" DUPLICATE_VEHICLE_NUMBER "$(echo "$BODY" | field code)"
echo "     $BODY"

echo "== Validation error"
call POST /vehicles '{"vehicleNumber":"DHA 1","company":"","type":"Spaceship","brand":"Toyota","model":"Hiace","status":"ACTIVE"}'
check "invalid → 400" 400 "$CODE"

echo "== Delete $ID"
call DELETE "/vehicles/$ID"
check "delete → 204" 204 "$CODE"
call GET "/vehicles/$ID"
check "get after delete → 404" 404 "$CODE"

echo; echo "Result: $pass passed, $fail failed"
[ "$fail" -eq 0 ]
