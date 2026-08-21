#!/usr/bin/env bash
set -Eeuo pipefail

required_vars=(TARGET_IP BASE_URL TEST_ID EVIDENCE SJA_SELECTED_PORTS)
for name in "${required_vars[@]}"; do
  if [[ -z "${!name:-}" ]]; then
    printf 'Missing environment variable: %s\n' "$name" >&2
    printf 'Run: source $HOME/sja-kali.env\n' >&2
    exit 2
  fi
done

if [[ "$TARGET_IP" != '192.168.255.128' ]]; then
  printf 'Refusing to scan unexpected target: %s\n' "$TARGET_IP" >&2
  printf 'This baseline is intentionally locked to 192.168.255.128.\n' >&2
  exit 3
fi

required_commands=(curl nmap)
for command_name in "${required_commands[@]}"; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    printf 'Required command not found: %s\n' "$command_name" >&2
    exit 4
  fi
done

umask 077
mkdir -p "$EVIDENCE"/{00-scope,01-network,02-http}

date -Is > "$EVIDENCE/00-scope/start-time.txt"
{
  printf 'target=%s\n' "$TARGET_IP"
  printf 'base_url=%s\n' "$BASE_URL"
  printf 'tester_ip=%s\n' "$(hostname -I 2>/dev/null | awk '{print $1}')"
  printf 'test_id=%s\n' "$TEST_ID"
} > "$EVIDENCE/00-scope/scope.txt"

printf '[1/7] Connectivity\n'
ping -c 3 "$TARGET_IP" > "$EVIDENCE/01-network/ping.txt" 2>&1 || true
curl -sS --connect-timeout 5 --max-time 15 \
  -o /dev/null \
  -w 'http=%{http_code} connect=%{time_connect}s total=%{time_total}s\n' \
  "$BASE_URL/" > "$EVIDENCE/01-network/http-connectivity.txt"

printf '[2/7] Selected TCP ports\n'
sudo nmap -Pn -sS -sV -T3 \
  -p "$SJA_SELECTED_PORTS" \
  -oA "$EVIDENCE/01-network/selected-ports" \
  "$TARGET_IP"

printf '[3/7] Home page and headers\n'
curl -sS --connect-timeout 5 --max-time 15 \
  -D "$EVIDENCE/02-http/home.headers.txt" \
  -o "$EVIDENCE/02-http/home.body.html" \
  "$BASE_URL/"

printf '[4/7] HTTP methods\n'
for method in GET HEAD OPTIONS TRACE PUT DELETE PATCH; do
  {
    printf '===== %s =====\n' "$method"
    curl -sS --connect-timeout 5 --max-time 15 \
      -i -X "$method" "$BASE_URL/" | sed -n '1,25p'
    printf '\n'
  } >> "$EVIDENCE/02-http/methods.txt"
done

printf '[5/7] CORS preflights\n'
curl -sS --connect-timeout 5 --max-time 15 \
  -i -X OPTIONS "$BASE_URL/api/v1/user/profile" \
  -H 'Origin: https://evil.example' \
  -H 'Access-Control-Request-Method: GET' \
  -H 'Access-Control-Request-Headers: Authorization,Content-Type' \
  > "$EVIDENCE/02-http/cors-evil.txt"

curl -sS --connect-timeout 5 --max-time 15 \
  -i -X OPTIONS "$BASE_URL/api/v1/user/profile" \
  -H "Origin: $BASE_URL" \
  -H 'Access-Control-Request-Method: GET' \
  -H 'Access-Control-Request-Headers: Authorization,Content-Type' \
  > "$EVIDENCE/02-http/cors-valid.txt"

printf '[6/7] Anonymous access matrix\n'
paths=(
  '/api/v1/user/profile'
  '/api/v1/users'
  '/api/v1/fee/list'
  '/api/v1/fee/standards'
  '/api/v1/system/logs'
  '/api/v1/upload/avatar/00000000-0000-0000-0000-000000000000.jpg'
  '/v3/api-docs'
  '/swagger-ui/index.html'
  '/.env'
  '/backup.sql'
)

for path in "${paths[@]}"; do
  code="$(curl -sS --connect-timeout 5 --max-time 15 \
    -o /dev/null -w '%{http_code}' "$BASE_URL$path")"
  printf '%-85s %s\n' "$path" "$code"
done > "$EVIDENCE/02-http/anonymous-matrix.txt"

printf '[7/7] Host and forwarded-address handling\n'
curl -sS --connect-timeout 5 --max-time 15 \
  -i -X POST "$BASE_URL/api/v1/auth/captcha/generate" \
  -H 'Host: evil.example' \
  > "$EVIDENCE/02-http/host-evil.txt"

curl -sS --connect-timeout 5 --max-time 15 \
  -i -X POST "$BASE_URL/api/v1/auth/captcha/generate" \
  -H 'X-Forwarded-For: 127.0.0.1' \
  > "$EVIDENCE/02-http/x-forwarded-for-loopback.txt"

date -Is > "$EVIDENCE/00-scope/end-time.txt"

printf '\nSafe baseline complete. Evidence: %s\n' "$EVIDENCE"
printf 'Review selected ports, anonymous matrix, CORS, methods, and headers manually.\n'
